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
# Setup environment (copies config files to BASEPATH if not exists)
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode
./setup-env.sh

# Start middleware
docker compose -f docker-compose-demo-env.yml up -d

# Add optional profiles
docker compose -f docker-compose-demo-env.yml --profile kafka --profile elastic --profile mongo up -d
```

#### Docker Config Management Rules
- All middleware config files must be placed under `BASEPATH` (not in the repository)
- Source configs are stored in `platform/docker/demo/conf/` as templates
- Run `setup-env.sh` to copy configs to BASEPATH if they don't exist
- BASEPATH structure:
  ```
  ${BASEPATH}/
    mysql/conf/my.cnf
    seata/conf/application.yml
    redis/conf/redis.conf
    kafka/data/
    elasticsearch/data/
    mongodb/data/
    minio/data/
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

## Docker Service Port Rules

### Internal vs External Ports
| Service | Internal Port | External Port |
|---------|--------------|---------------|
| BFF | 8080 | 18080 |
| order-service | 8082 | 18081 |
| storage-service | 8083 | 18082 |
| account-service | 8083 | 18083 |
| payment-service | 8084 | 18090 |
| es-service | 8086 | 8086 |

### Service URL Environment Variables
Docker コンテナ間通信では**コンテナ名 + 内部ポート**を使用:
```yaml
environment:
  - ORDER_SERVICE_BASE_URL=http://ec-demo-order-service:8082
  - STORAGE_SERVICE_BASE_URL=http://ec-demo-storage-service:8083
  - ACCOUNT_SERVICE_BASE_URL=http://ec-demo-account-service:8083
  - PAYMENT_SERVICE_BASE_URL=http://ec-demo-payment-service:8084
  - ES_SERVICE_BASE_URL=http://ec-demo-es-service:8086
```

### Common Port Mistakes
- ❌ `localhost` をコンテナ間通信に使用（→ コンテナ名を使用）
- ❌ 外部ポートをコンテナ間通信に使用（→ 内部ポートを使用）
- ❌ application.yml のポートと docker-compose マッピングの不一致

## Demo Environment Initialization

### Full Setup Commands
```bash
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode

# 1. Setup config files
./setup-env.sh

# 2. Start middleware (all profiles)
docker compose -f docker-compose-demo-env.yml \
  --profile kafka --profile elastic --profile mongo --profile minio up -d

# 3. Initialize ES index & import products
cd elasticsearch
./init-es-products-index.sh
./init-upload-product-minio.sh
cd ..

# 4. Start applications
docker compose -f docker-compose-demo-app.yml --profile elastic up -d
```

### Verify
```bash
# ES search
curl "http://localhost/ec-api/api/products/search?q=iphone"

# MinIO image
curl -I "http://localhost:9000/ec-demo/product/images/1001.jpg"
```
