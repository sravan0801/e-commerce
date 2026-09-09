# Architecture

## Shape

One Spring Boot app, split into packages by concern. The only asynchronous seam
is order fulfilment: the web layer saves an order and drops an event on Kafka;
a consumer in the same app picks it up and finishes the work.

```
com.example.storefront
├── common        BaseEntity, error handling, PageResponse
├── config        AppProperties, OpenAPI, the Kafka topic
├── security      JWT filter, register/login, users, roles
├── catalog       products (price, stock, @Version) + categories
├── order         order entity + REST + OrderProcessor (the Kafka consumer)
├── event         OrderPlacedEvent (the one message type)
└── bootstrap     dev-only account seeding
```

## The order flow

```
POST /api/orders
      │  (OrderService, one transaction)
      │  validate items, save order as PENDING, publish OrderPlacedEvent
      ▼
  Kafka topic "orders.placed"
      ▼
  OrderProcessor  (@KafkaListener, one transaction)
      │  every line has stock?           no ──▶ order REJECTED
      │  amount under the payment limit? no ──▶ order REJECTED
      │  yes: decrement stock, order CONFIRMED
```

The HTTP response comes back as soon as the order row is committed, so the
client sees `PENDING` and polls `GET /api/orders/{ref}` until it turns
`CONFIRMED` or `REJECTED` a moment later.

Why Kafka here at all? It decouples "take the order" from "fulfil the order":
the API stays fast and stays up even if fulfilment is slow, and the processor
can be scaled or restarted independently. It also gives redelivery for free — if
`OrderProcessor` throws, the message is retried.

## What is deliberately simple

- **Publish, not outbox.** `OrderService` writes the DB row and then calls
  `KafkaTemplate.send`. Those are two systems, so a crash in the gap could leave
  an order stuck at `PENDING`. Production would use the *transactional outbox*
  pattern (write the event to a DB table in the same transaction, relay it to
  Kafka separately). Left out on purpose.
- **No dedupe table.** Kafka is at-least-once, so `OrderProcessor` could see a
  message twice. It guards against that cheaply by checking
  `status == PENDING` before doing anything.
- **Stock lives on `product`.** A `stock_quantity` column, decremented in the
  processor. `@Version` on `Product` gives optimistic locking, so two orders
  racing for the last unit can't both succeed. No separate inventory ledger.
- **Payment is a stand-in.** Orders at or above a fixed amount are "declined";
  everything else is "approved". No gateway, no payment records.
- **Access token only.** Login returns one JWT (24 h). No refresh tokens.

## Money

Integer minor units (`priceCents`, `long`) everywhere, with an ISO-4217
`currency` string next to every amount. No `float`/`double` in the domain. An
order requires a single currency across its lines.

## Database ownership

Hibernate runs with `ddl-auto: validate` — it never changes the schema. Flyway
(`src/main/resources/db/migration`) owns it: `V1__schema.sql` creates every
table, `V2__seed_catalog.sql` fills the catalogue. User accounts are seeded in
code (they need the `PasswordEncoder`) — see `bootstrap/AccountSeeder`.
