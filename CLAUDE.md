# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

E-commerce payment system demo integrating PayPay payments with microservices architecture. Uses **Seata Saga** for distributed transaction management and **Webhook + Polling** hybrid for robust payment status synchronization.

## Build & Run Commands

### Backend (Java/Maven)
```bash
# Full build (all modules)
mvn clean package -DskipTests

# Run tests
mvn test

# Run single service
mvn spring-boot:run -pl apps/bff
mvn spring-boot:run -pl apps/services/order-service
mvn spring-boot:run -pl apps/services/storage-service
mvn spring-boot:run -pl apps/services/account-service
mvn spring-boot:run -pl apps/services/payment-service
mvn spring-boot:run -pl apps/services/alert-service
mvn spring-boot:run -pl apps/services/es-service

# Run single module test
mvn test -pl apps/services/order-service
```

### Frontend (Vue 3 / pnpm)
```bash
cd apps/web
pnpm install
pnpm dev        # Development server
pnpm build      # Production build
```

### Infrastructure (Docker)
```bash
# Start local middleware (MySQL, Redis, Seata)
cd platform/docker/local
docker compose up -d

# Add optional Kafka + Elasticsearch
docker compose --profile kafka --profile elastic up -d
```

### Test Scripts
```bash
# Test order creation via Saga
./scripts/test-saga.sh [quantity] [productId]

# Health checks
curl http://localhost:8080/actuator/health  # BFF
curl http://localhost:8082/actuator/health  # order-service
```

### Environment Setup
```bash
./init.sh pull  # Pull env files from shared location
```

## Architecture

### Monorepo Structure
```
apps/
  bff/               # Backend-for-Frontend (port 8080) - WebSocket, PayPay webhook, Redis session
  services/
    order-service/   # (port 8082) Order state machine, Saga orchestration
    storage-service/ # (port 8083) Inventory management
    account-service/ # (port 8081) Account/balance
    payment-service/ # (port 8084) PayPay integration
    alert-service/   # (port 8085) Kafka Streams consistency checks
    es-service/      # (port 8086) Elasticsearch product search
  web/               # Vue 3 + TypeScript frontend
platform/docker/     # Docker Compose configs (local vs demo/VPS)
docs/adr/            # Architectural Decision Records
```

### Hybrid Hexagonal Architecture
Each service follows this package structure under `com.demo.ec.[service]`:
```
web/          # REST controllers, DTOs (Inbound Adapter)
application/  # Use case orchestration, transaction boundaries
domain/       # Pure business logic - NO external dependencies
gateway/      # MyBatis mappers, Feign clients (Outbound Adapter)
config/       # Spring configuration
```

**Dependency rule**: `web → application → domain ← gateway`. Domain NEVER depends outward.

## Critical Design Patterns

### Saga Workflows (Seata)
- State machines in `apps/services/order-service/src/main/resources/statelang/*.json`
- Never use `@GlobalTransactional` with Saga mode
- Order flow: `PENDING → WAITING_PAYMENT → PAID/FAILED`

### Payment Synchronization
- **Primary**: PayPay Webhook with signature verification
- **Fallback**: Polling for `WAITING_PAYMENT` orders
- **Idempotency**: `payment_last_event_id` prevents duplicate processing
- PAID state is terminal - ignore late failure events

### Consistency Alerts (alert-service)
Kafka Streams detects order/payment inconsistencies:
- Rule A: Payment succeeded but order not updated
- Rule B: Order marked PAID but payment failed
- Rule C: Duplicate payments (same orderId)

## Code Guidelines

### Forbidden
- Spring/JPA annotations in `domain/` packages
- `@GlobalTransactional` with Saga state machines
- Business logic in `web/` layer
- Direct database access from `application/` layer

### Required
- Domain entities: pure POJOs, no framework annotations
- Gateway interfaces defined in `domain/` or `application/`, implemented in `gateway/`
- MyBatis-Plus: use `LambdaQueryWrapper` for type-safe queries
- Design for idempotency (webhooks arrive multiple times)
- Use `record` for DTOs and domain events (Java 21)

## Tech Stack
- Java 21, Spring Boot 3.2.8, MyBatis-Plus 3.5.8
- Seata 2.0 (Saga mode), Kafka Streams
- Firebase Authentication (ID token verification in BFF)
- Redis (session storage), MySQL 8.0, MongoDB (audit logs)
- Elasticsearch 8.x, MinIO (S3-compatible storage)

## Key References
- ADRs: `docs/adr/` (read before making architectural changes)
- Architecture details: `docs/architecture/README_ARCHITECTURE.md`
- Local setup: `docs/runbook/README_LOCAL_SETUP.md`
- Saga example: `apps/services/order-service/src/main/resources/statelang/order_create_saga.json`
