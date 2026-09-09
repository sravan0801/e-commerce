# Walkthrough

Read the code in this order. Each section says which files to open and how to
prove to yourself it works.

---

## 0. Boot the stack

```bash
docker compose up --build
```

Wait for `storefront-app` to log `Started StorefrontApplication`. Open
http://localhost:8080/swagger and http://localhost:8081 (Kafka-UI). Leave both
open while you read.

---

## 1. Database: JPA + Flyway

**Files:** `src/main/resources/db/migration/V1__schema.sql`,
`V2__seed_catalog.sql`, `common/BaseEntity.java`, `config/PersistenceConfig.java`,
`catalog/Product.java`.

- **Flyway owns the schema.** `application.yml` sets
  `spring.jpa.hibernate.ddl-auto: validate` — Hibernate checks its entity
  mappings against the tables at startup and refuses to boot if they disagree,
  but never issues `CREATE`/`ALTER`. Every schema change is a new `V*.sql` file.
- **`BaseEntity`** centralises `id` + `created_at` + `updated_at`.
  `@EntityListeners(AuditingEntityListener.class)` + `@EnableJpaAuditing` fill
  the timestamps.
- **`Product.version`** is `@Version` — JPA optimistic locking. If two
  transactions load the same product and both save, the second gets an
  `OptimisticLockingFailureException` instead of silently overwriting.
- **Money** is `long` cents (`price_cents`), never a `double`.

**Prove it:** `GET /api/catalog/products` returns the six seeded products.
`docker compose exec postgres psql -U storefront -c '\dt'` lists the tables;
`select * from flyway_schema_history;` shows the two migrations.

---

## 2. Security: stateless JWT

**Files:** `security/SecurityConfig.java`, `JwtService.java`,
`JwtAuthenticationFilter.java`, `AppUserDetailsService.java`,
`AppUserPrincipal.java`, `AuthService.java`, `AuthController.java`.

- **`SecurityConfig`** — `SessionCreationPolicy.STATELESS` (no `JSESSIONID`),
  CSRF off (safe with no cookie session), a public allow-list (auth endpoints,
  catalogue reads, docs, health), then `anyRequest().authenticated()`.
  `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
  `@EnableMethodSecurity` turns on the `@PreAuthorize("hasRole('ADMIN')")` on
  `CatalogService.addProduct`.
- **Login** — `AuthService.login` calls the `AuthenticationManager`
  (`AppUserDetailsService` + `BCryptPasswordEncoder`), then `JwtService` mints a
  signed HS256 token carrying `sub`, `uid`, `authorities`, `iss`, `exp`. That is
  the whole token story — there is no refresh token; when it expires you log in
  again.
- **Request path** — `JwtAuthenticationFilter` reads `Authorization: Bearer`,
  `JwtService.parse` verifies signature + issuer + expiry, and an
  `AppUserPrincipal` goes into the `SecurityContext`. No DB hit per request.

**Prove it:**

```bash
# bad password -> 401
curl -si localhost:8080/api/auth/login -H 'content-type: application/json' \
  -d '{"email":"demo@storefront.test","password":"wrong"}' | head -1

# protected endpoint with no token -> 401
curl -si localhost:8080/api/orders | head -1
```

Decode an access token at https://jwt.io to see the claims.

---

## 3. Kafka: place an order, let a consumer finish it

**Files:** `order/OrderService.java`, `event/OrderPlacedEvent.java`,
`config/KafkaTopicConfig.java`, `order/OrderProcessor.java`, `order/Order.java`,
`order/OrderStatus.java`.

1. **`OrderController.place` → `OrderService.placeOrder`** (one transaction):
   loads the products, checks they're active and share a currency, snapshots sku
   + unit price onto each `OrderLine`, saves the `Order` as `PENDING`, then
   `kafkaTemplate.send("orders.placed", orderRef, event)`. HTTP returns `201`
   with status `PENDING` immediately.
2. **`OrderProcessor.onOrderPlaced`** (`@KafkaListener`, group
   `order-processor`, one transaction):
   - if the order isn't `PENDING` any more, return — that's the idempotency
     guard against Kafka redelivering a message;
   - if any line is short on stock → `order.markRejected(...)`, done;
   - if the total is at or above the payment limit ($10,000) → rejected (the
     stand-in "gateway");
   - otherwise decrement `stock_quantity` on each product and
     `order.markConfirmed()`.
   If this method throws, the transaction rolls back and Kafka re-delivers.

**Prove the happy path:**

```bash
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'content-type: application/json' \
  -d '{"email":"demo@storefront.test","password":"demo12345"}' | jq -r .accessToken)

REF=$(curl -s localhost:8080/api/orders -H "authorization: Bearer $TOKEN" \
  -H 'content-type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2}]}' | jq -r .orderRef)

sleep 2
curl -s localhost:8080/api/orders/$REF -H "authorization: Bearer $TOKEN" | jq '.status,.rejectionNote'
# "CONFIRMED", null
```

**Prove rejection (not enough stock):** order `quantity: 9999` for product 1 →
status becomes `REJECTED`, `rejectionNote` starts with `Out of stock`.

**Prove the payment limit:** order enough of product 3 to exceed $10,000 →
`REJECTED`, `rejectionNote` is `Payment declined: amount too large`. Watch the
message land on `orders.placed` in Kafka-UI either way.

---

## 4. Docker

**Files:** `Dockerfile`, `docker-compose.yml`, `application-docker.yml`.

- **Multi-stage `Dockerfile`** — stage 1 (`maven:3.9-eclipse-temurin-21`)
  builds the jar; stage 2 (`eclipse-temurin:21-jre-jammy`) carries only the JRE
  + jar and runs as a non-root user.
- **`pom.xml` is copied before `src`** so `dependency:go-offline` is its own
  cached layer — editing source doesn't re-download the dependencies.
- **`docker-compose.yml`** — Postgres, Kafka (KRaft, no ZooKeeper), Kafka-UI,
  and the app. The app waits on `condition: service_healthy` for both
  dependencies. Config reaches the app as env vars (`SPRING_DATASOURCE_URL`,
  `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `APP_JWT_SECRET`).

**Prove it:** `docker compose up --build` from a clean checkout gives a working
API with no local Java or Maven install.

---

## 5. Tests

**Files:** `src/test/java/.../support/AbstractIntegrationTest.java`,
`order/OrderProcessingIntegrationTest.java`,
`security/AuthFlowIntegrationTest.java`.

- `AbstractIntegrationTest` starts one Postgres and one Kafka container
  (Testcontainers) and wires Spring to them with `@ServiceConnection`.
- `OrderProcessingIntegrationTest` places an order and uses **Awaitility** to
  poll until the consumer drives it to `CONFIRMED`, then checks stock dropped. A
  second test checks the short-stock path ends `REJECTED`. Both exercise Kafka,
  the listener, and the DB together.

```bash
mvn test        # Docker must be running
```

---

## Ideas to extend it

1. **Add the transactional outbox** — write the event to an `outbox` table in
   `placeOrder`'s transaction, relay it with a `@Scheduled` poller. Removes the
   dual-write gap.
2. **Swap the stand-in payment** for a real HTTP call behind an interface.
3. **Split `OrderProcessor` into its own Spring Boot app** that shares only the
   `event` package — the topic is already the seam.
