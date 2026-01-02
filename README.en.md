# EC Payment System Demo

This is a microservices demo of an EC payment flow integrating PayPay. It is designed for real-world payment asynchrony: **Seata Saga** for eventual consistency, and **Webhook + polling** for robust state transitions.

> 日本語: `README.md` / 中文: `README.zh-CN.md`

## Tech-lead design highlights

- **BFF + WebSocket**: Concentrates UI-optimized APIs and realtime notifications in the BFF to maximize change resilience and UX
- **Seata Saga**: Implements cross-service consistency with a state machine + compensations (operationally manageable eventual consistency)
- **Hybrid monitoring**: Webhook-first with polling fallback to handle missing/late external events
- **Idempotency**: Uses `payment_last_event_id` to absorb duplicates and out-of-order events safely
- **kafka-alert**: Detects Rule A/B/C inconsistencies via Kafka Streams and emits operational alerts (`AlertRaised`)
  - **Rule A**: Payment successful but order not reflected (e.g., PayPay payment completed → order stays `WAITING_PAYMENT`)
  - **Rule B**: Order `PAID` but payment failed (e.g., order paid → PayPay shows `FAILED`)
  - **Rule C**: Payment succeeds after expiry/failure (e.g., order `FAILED` → delayed PayPay success notification)

## Outcome Metrics (Value demonstrated in demo)

- **Payment completion → order reflection**: Target *within 60 seconds (99%)* for `PAID` reflection (webhook priority / polling convergence)
- **Resilience to external event loss**: Even without webhook delivery, polling converges to `PAID/FAILED` (monitors pending orders)
- **Consistency visualization**: Inconsistencies detected as Rule A/B/C, outputs `AlertRaised` to `alerts.order_payment_inconsistency.v1`

## Non-functional design decisions (summary)

- **Observability**: Standardized `actuator/health` across all services to accelerate troubleshooting
- **Resilience**: Webhook-first + polling fallback, idempotency, compensation transactions for safe convergence
- **Security**: Firebase ID Token verification + Redis sessions, cookies assume `HttpOnly` / `SameSite` / `Secure`
- **Fail-safe**: Return 503 for critical dependency failures like Redis, fail safely (details in `_docs/`)

## Risks and countermeasures (excerpt)

| Risk | Impact | Countermeasure |
|---|---|---|
| PayPay webhook missing/late | Orders stuck in `WAITING_PAYMENT` | Polling convergence (webhook priority) |
| Webhook resend/duplicate events | Double updates・consistency breakdown | Idempotency via `payment_last_event_id` |
| Success/failure events arrive out-of-order | Failure arrives after `PAID`, etc. | Record-only for failures after `PAID`, no state change |
| Saga mid-failure (stock/order/payment inconsistency) | Eventual consistency breakdown | Compensation rollback, kafka-alert detection |
| Redis session failure | Auth/authorization degradation | Return 503 for `/api/**` (fail safe) to prompt recovery |

## Demo highlights (3 steps)

1. **Purchase request**: Create order with `./test-saga.sh` (`PENDING → WAITING_PAYMENT`)
2. **Payment event reflection**: Webhook (priority) or polling (fallback) converges to `PAID/FAILED`
3. **Observation**: Immediate screen updates via WebSocket, confirm Rule A/B/C `AlertRaised` in topics (when kafka-alert is running)

## Services

- **BFF (Backend for Frontend)** (port 8080)
  - WebSocket realtime updates
  - PayPay integration (webhook ingestion/verification → propagate to order-service)
  - Redis session (Firebase ID Token verification → issue `sid`)
- **order-service** (port 8081)
  - Order state machine (`PENDING → WAITING_PAYMENT → PAID/FAILED`)
  - Saga orchestration (stock confirm/compensate)
  - Unified payment status handling for both webhook and polling
- **storage-service** (port 8082)
  - Stock reserve/confirm/compensate
- **account-service** (port 8083)
  - Account/balance management

## Tech stack (kept in README)

### Frontend
- **Vue 3**
- **TypeScript**
- **Vite**

### Backend
- **JDK 17**
- **Spring Boot 3.x**
- **Modern microservices**
- **Distributed transaction (Saga)** - Seata Saga mode
- **WebSocket**
- **Spring Cloud OpenFeign**

### Infra / Middleware
- **Docker / Docker Compose**
- **MySQL 8.0**
- **Seata 2.0**
- **Redis**
- **Firebase Authentication**
- **Kafka / Kafka Streams** - Detect Rule A/B/C and emit `AlertRaised` to `alerts.order_payment_inconsistency.v1`
- **Elasticsearch**
- **MyBatis-Plus**

### Observability
- **Spring Boot Actuator**

## Diagrams & docs (see `_docs/` for details)

- Draw.io diagram: `docs/ec-demo-architecture.drawio` (pages: Overview / DataFlow)
- Local startup guide: `_docs/runbook/README_LOCAL_SETUP.md`
- Architecture deep dive (Saga scope, state machine, kafka-alert contract, non-functional): `_docs/architecture/README_ARCHITECTURE.md`
- Deployment guide (VPS-oriented): `_docs/docker/demo/deploy.md`
