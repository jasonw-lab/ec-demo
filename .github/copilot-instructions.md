# EC Demo - AI Coding Agent Instructions

## Architecture Overview

This is an **e-commerce payment system** demo using **Hybrid Hexagonal Architecture** in a Java Spring Boot monorepo. The system integrates external payment providers (PayPay) and ensures eventual consistency across distributed services using **Seata Saga**.

**Core architectural principle**: Domain logic must remain isolated from infrastructure concerns. All business logic lives in `domain` packages with zero external dependencies.

### Monorepo Structure
- `apps/services/` - Independent microservices (order, payment, storage, account, alert, es-service)
- `apps/bff/` - Backend-for-Frontend with WebSocket for real-time notifications
- `apps/web/` - Vue 3 + TypeScript frontend
- `libs/` - Shared domain utilities (minimal - prefer service independence)
- `platform/docker/` - Docker Compose configurations (local dev vs. VPS deployment)
- `docs/adr/` - Architectural Decision Records (read these first for design rationale)

### Package Structure (Hybrid Hexagonal)
Each service follows this **mandatory** layer structure under `com.demo.ec.[service]`:

```
web/          → REST controllers, DTOs, validation (Inbound Adapter)
application/  → Use case orchestration, transaction boundaries
domain/       → Pure business logic - NO Spring, DB, or external dependencies
gateway/      → MyBatis mappers, Feign clients, external integrations (Outbound Adapter)
config/       → Spring configuration
```

**Dependency rule**: Flow is `web → application → domain ← gateway`. Domain never depends outward.

## Critical Design Patterns

### 1. Saga-Based Distributed Transactions
- **Seata Saga** manages multi-service workflows with compensation
- State machines defined in JSON: `apps/services/order-service/src/main/resources/statelang/*.json`
- **Never use `@GlobalTransactional` with Saga** - Saga manages its own transaction boundaries
- Example: Order creation → inventory reservation → payment → confirmation/compensation

### 2. Payment Status Synchronization (Webhook + Polling Hybrid)
**Problem**: External payment webhooks may be lost or delayed.

**Solution**: Dual mechanism (both implemented in BFF and order-service):
- **Primary**: Webhook from PayPay with signature verification
- **Fallback**: Polling for orders in `WAITING_PAYMENT` state
- **Idempotency**: `payment_last_event_id` prevents duplicate event processing
- **State machine**: `PENDING → WAITING_PAYMENT → PAID/FAILED` (PAID state is terminal - ignore late failure events)

### 3. Real-Time Notifications via WebSocket
- BFF exposes `/ws/orders` endpoint for order status updates
- Channel authentication via token-based session management
- Pushes updates immediately on payment status changes
- Frontend maintains WebSocket connection for live order tracking

### 4. Consistency Validation with Kafka Streams (alert-service)
**Purpose**: Detect and alert on order/payment inconsistencies in distributed systems.

**Rules** (implemented in alert-service using Kafka Streams):
- **Rule A**: Payment succeeded but order not updated (e.g., COMPLETED payment → order still `WAITING_PAYMENT`)
- **Rule B**: Order marked `PAID` but payment failed/cancelled
- **Rule C**: Payment success after order expiry/failure

Alerts published to `alerts.order_payment_inconsistency.v1` topic with `AlertRaised` events.

## Development Workflows

### Local Development Setup
```bash
# 1. Start infrastructure only (recommended for development)
cd platform/docker/local
docker compose up -d

# 2. Optional: Add Kafka + Elasticsearch (heavy - only when needed)
docker compose --profile kafka --profile elastic up -d

# 3. Initialize environment files (if needed)
./init.sh pull  # Pull from shared env directory

# 4. Run services from IDE or:
mvn spring-boot:run -pl apps/bff
mvn spring-boot:run -pl apps/services/order-service
# etc.
```

**Port mapping (local dev)**:
- MySQL: 3307, Seata: 8092/7092, Redis: 6379, Kafka: 9092 (optional), Elasticsearch: 9200 (optional)
- BFF: 8080, order-service: 8082, storage-service: 8083, account-service: 8081, payment-service: 8084, alert-service: 8085, es-service: 8086

### Testing Critical Flows
```bash
# Test order creation + Saga flow
./test-saga.sh [quantity] [productId]

# Verify service health
curl http://localhost:8082/actuator/health  # order-service
curl http://localhost:8080/actuator/health  # BFF
```

### Building
```bash
# Multi-module Maven build
mvn clean package -DskipTests

# Run tests
mvn test
```

## Code Guidelines

### When Adding New Features
1. **Check ADRs first**: Read `docs/adr/` for context on architectural decisions
2. **Preserve layer boundaries**: 
   - Business logic → `domain/` (pure Java, testable without Spring)
   - Database/external calls → `gateway/` (implement interfaces defined in domain/application)
   - Controllers → `web/` (thin layer, delegate to application services)
3. **Saga workflows**: Define new workflows in `statelang/*.json` with compensation actions
4. **Cross-service calls**: Use Feign clients in `gateway/`, never directly in domain
5. **Idempotency**: Always design for duplicate events (payment webhooks can arrive multiple times)

### Forbidden Patterns
- ❌ Importing Spring/JPA annotations in `domain/` packages
- ❌ Using `@GlobalTransactional` with Saga state machines (conflicts with Saga compensation)
- ❌ Business logic in controllers (`web/` layer)
- ❌ Direct database access from `application/` layer (use gateway interfaces)
- ❌ Shared libraries for service-specific logic (maintain service independence)

### Common Patterns
- ✅ Domain entities in `domain/` extend nothing (pure POJOs)
- ✅ Gateway interfaces (Ports) defined in `domain/` or `application/`, implemented in `gateway/`
- ✅ Use MyBatis-Plus `LambdaQueryWrapper` for type-safe queries
- ✅ Actuator health endpoints for all services (`/actuator/health`)
- ✅ Firebase ID Token validation in BFF → Redis session (`sid` cookie)
- ✅ WebSocket channels authenticated via session tokens

## Key Files to Reference

**Architecture documentation**:
- [docs/adr/001-adoption-of-hybrid-hexagonal-monorepo.md](docs/adr/001-adoption-of-hybrid-hexagonal-monorepo.md) - Why Hybrid Hexagonal
- [docs/architecture/README_ARCHITECTURE.md](docs/architecture/README_ARCHITECTURE.md) - Detailed architecture + data flows
- [docs/guide/design-standards.md](docs/guide/design-standards.md) - Development standards

**Saga examples**:
- [apps/services/order-service/src/main/resources/statelang/order_create_saga.json](apps/services/order-service/src/main/resources/statelang/order_create_saga.json)
- [apps/services/order-service/src/main/java/com/demo/ec/order/application/OrderSagaServiceImpl.java](apps/services/order-service/src/main/java/com/demo/ec/order/application/OrderSagaServiceImpl.java)

**Payment integration**:
- BFF webhook handler: `apps/bff/src/main/java/com/demo/ec/bff/gateway/paypay/PayPayWebhookController.java`
- Order payment service: `apps/services/order-service/src/main/java/com/demo/ec/order/application/OrderPaymentService.java`

**WebSocket implementation**:
- [apps/bff/src/main/java/com/demo/ec/bff/config/WebSocketConfig.java](apps/bff/src/main/java/com/demo/ec/bff/config/WebSocketConfig.java)
- [apps/bff/src/main/java/com/demo/ec/bff/gateway/websocket/OrderStatusWebSocketHandler.java](apps/bff/src/main/java/com/demo/ec/bff/gateway/websocket/OrderStatusWebSocketHandler.java)

## Tech Stack Specifics
- **Java 21** (LTS baseline - use modern features)
- **Spring Boot 3.2.8** with Spring Cloud OpenFeign
- **Seata 2.0** (Saga mode for distributed transactions)
- **MyBatis-Plus 3.5.8** (use `LambdaQueryWrapper` for type safety)
- **Firebase Authentication** (ID token verification in BFF)
- **Redis** for session storage (fail-safe: return 503 if Redis unavailable)
- **Kafka Streams** for event-driven consistency checks
- **Elasticsearch 8.x** for product search (handled by es-service)

## Coding Rules
- Follow Google Java Style
- Prefer explicit, readable code over clever code
- Avoid over-engineering, but keep extensibility

## Observability
All services expose Spring Boot Actuator:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`

Fail-safe principle: If critical dependencies (Redis, DB) are down, return **503 Service Unavailable** rather than degraded/incorrect responses.

## AI Development Notes
This project explicitly embraces AI-assisted development (Cursor, GitHub Copilot). When proposing changes:
1. Reference relevant ADRs to maintain architectural consistency
2. Preserve the layer boundaries - AI should suggest domain-first designs
3. Consider distributed system concerns (idempotency, eventual consistency, compensation)
4. Test distributed transaction flows, not just unit tests

## AI Prompting Preferences

- **Java 21 Usage**:
  Prefer `record` for DTOs and domain events.
  Use `var` for local variables when it improves readability.

- **Defensive Design**:
  When implementing `gateway` or `application` logic,
  always consider failure scenarios and duplicate event delivery.
  Design idempotent operations by default.

- **Test Generation**:
  When generating tests, include failure and compensation flows
  for Saga-based transactions, not only the happy path.
