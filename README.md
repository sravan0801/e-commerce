# Storefront

A small Spring Boot e-commerce backend built to practise four things together:

| Concern   | What it uses                                                        |
|-----------|--------------------------------------------------------------------|
| Database  | PostgreSQL, Spring Data JPA, Flyway migrations, `@Version` locking |
| Messaging | Kafka — an order is placed synchronously, then fulfilled by a consumer |
| Security  | Stateless JWT (access token only), BCrypt, method-level roles     |
| Packaging | Multi-stage Docker build, `docker compose` for the whole stack    |

The domain is **catalog + orders**. `POST /api/orders` saves the order as
`PENDING` and publishes one event; a `@KafkaListener` in the same app
(`OrderProcessor`) checks stock, runs a stand-in payment, and moves the order to
`CONFIRMED` or `REJECTED`.

```
POST /api/orders ──▶ save PENDING ──▶ Kafka "orders.placed"
                                          │
                                          ▼
                                    OrderProcessor
                                      stock ok?  payment ok?
                                      ├── yes ──▶ decrement stock, order CONFIRMED
                                      └── no  ──▶ order REJECTED
```

## Run it

```bash
docker compose up --build
```

That starts Postgres, Kafka (KRaft, no ZooKeeper), Kafka-UI, and the app.

| URL                                    | What                        |
|----------------------------------------|-----------------------------|
| http://localhost:8080/swagger          | API explorer                |
| http://localhost:8080/actuator/health  | health                      |
| http://localhost:8081                  | Kafka-UI (topics, messages) |

Seeded accounts (`APP_SEED_ENABLED=true`, set in `docker-compose.yml`):

| email                   | password     | roles            |
|-------------------------|--------------|------------------|
| `admin@storefront.test` | `admin12345` | ADMIN + CUSTOMER |
| `demo@storefront.test`  | `demo12345`  | CUSTOMER         |

The catalogue is seeded by Flyway (`V2__seed_catalog.sql`).

## Try it

```bash
# 1. log in
TOKEN=$(curl -s localhost:8080/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"email":"demo@storefront.test","password":"demo12345"}' | jq -r .accessToken)

# 2. see products
curl -s localhost:8080/api/catalog/products | jq

# 3. place an order (productId 1, qty 2)
REF=$(curl -s localhost:8080/api/orders \
  -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2}]}' | jq -r .orderRef)

# 4. poll until CONFIRMED (a second or two)
curl -s localhost:8080/api/orders/$REF -H "authorization: Bearer $TOKEN" | jq
```

More requests, including the rejection paths, are in
[`docs/api-examples.http`](docs/api-examples.http).

## Local development (without Docker)

You need **JDK 21** and **Maven 3.9+**. Start only the infra with Docker, then
run the app:

```bash
docker compose up -d postgres kafka
mvn spring-boot:run
```

## Tests

```bash
mvn test
```

Integration tests spin up throwaway Postgres and Kafka containers via
Testcontainers, so Docker must be running. `OrderProcessingIntegrationTest`
places an order and waits for the consumer to confirm it.

## Where to read next

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — package map, the order flow,
  and what was kept deliberately simple.
- [`docs/WALKTHROUGH.md`](docs/WALKTHROUGH.md) — a guided tour of each subsystem,
  in the order worth learning them.
