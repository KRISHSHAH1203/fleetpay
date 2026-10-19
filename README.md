# FleetPay API

A Spring Boot REST service for fleet card payments. Companies issue cards to drivers, each card has a daily spend limit, and every payment is posted exactly once even if the client retries.

**Stack:** Java 17, Spring Boot 3, Spring Data JPA (Hibernate), MySQL, H2 for tests, JUnit 5, MockMvc, Docker, GitHub Actions.

## Run it

```bash
# Run all tests (uses in-memory H2, no setup needed)
mvn verify

# Run the app with MySQL
docker compose up --build
```

Then:

```bash
# Create a card with a 500 daily limit
curl -X POST localhost:8080/api/cards -H "Content-Type: application/json" \
  -d '{"driverName":"Asha","last4":"4242","dailyLimit":500}'

# Post a payment (send the same Idempotency-Key twice: only one charge)
curl -X POST localhost:8080/api/transactions -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f1c-001" -d '{"cardId":1,"amount":120.50,"merchant":"Shell Fuel"}'

# Paginated history and a spend report
curl "localhost:8080/api/cards/1/transactions?page=0&size=20"
curl "localhost:8080/api/reports/spend?from=2026-10-01T00:00:00Z&to=2026-10-31T23:59:59Z"
```

## Endpoints

| Method | Path | What it does |
|---|---|---|
| POST | `/api/cards` | Issue a card with a daily limit |
| GET | `/api/cards/{id}` | Get a card |
| POST | `/api/cards/{id}/deactivate` | Block a card |
| POST | `/api/transactions` | Post a payment. Needs an `Idempotency-Key` header. 201 first time, 200 with the original result on replay |
| GET | `/api/cards/{id}/transactions` | Paginated history, newest first (max 100 per page) |
| GET | `/api/reports/spend` | Total approved spend per card in a date range |
| GET | `/actuator/health` | Health check for monitoring |

## Design decisions (what to say in an interview)

**1. Idempotency.** Payment networks retry. If the same event arrives twice, charging twice is the worst possible bug. Every post carries an `Idempotency-Key`. The service looks it up first and returns the original result if it exists, and the database has a unique constraint on the key as a second line of defence.

**2. Concurrency.** Two payments on the same card at the same moment could both read "spent 250 of 300" and both pass. `CardRepository.findByIdForUpdate` takes a pessimistic row lock (`SELECT ... FOR UPDATE`) inside the transaction, so the second request waits for the first to commit, then sees the updated total.

**3. OOP and the strategy pattern.** Each check (card active, daily limit) is a `SpendRule` implementation. `TransactionService` just runs the list in order. Adding a new rule, say a merchant blocklist, means adding one class, with no change to existing code (open/closed principle). Encapsulation: entities expose behaviour like `card.deactivate()` instead of public setters. Polymorphism: the service calls `rule.check(...)` without knowing which rule it is.

**4. Query performance.** The daily-limit check runs on every payment, so the `transactions` table has a composite index on `(card_id, created_at)`. That turns "sum today's spend for this card" into an index range scan instead of a full table scan. Pagination is capped at 100 rows to protect the database.

**5. Transactions.** `@Transactional` on `post()` means the lock, the limit check and the insert are one atomic unit. Read-only methods use `@Transactional(readOnly = true)`.

**6. Errors.** `GlobalExceptionHandler` returns RFC 7807 problem responses: 400 for validation errors or a missing key, 404 for unknown cards, 409 for duplicate cards.

**7. Testability.** The `Clock` is injected, so date logic can be tested. Unit tests cover rules in isolation and integration tests go through HTTP, JPA and the database.

## Tests

- `DailyLimitRuleTest`: unit tests for the limit rule, no Spring context
- `TransactionApiIntegrationTest`: full API tests covering approval, idempotent replay (one row only), limit decline, inactive card decline, validation errors, and 404s

CI (`.github/workflows/ci.yml`) runs `mvn verify` and builds the Docker image on every push.

## Ideas to extend

- Optimistic locking with `@Version` instead of a row lock, and when you'd pick each
- Flyway migrations instead of `ddl-auto`
- A merchant category rule (fuel-only cards)
- Testcontainers to run integration tests against real MySQL
