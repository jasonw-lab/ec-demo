# Architecture Overview

## Monorepo Structure

This project has been migrated to a **Monorepo** structure with **Hybrid Hexagonal Architecture**.

```
branch-feature-one/
├── apps/
│   ├── services/          # Backend microservices
│   │   ├── account-service
│   │   ├── alert-service
│   │   ├── es-service
│   │   ├── order-service
│   │   ├── payment-service
│   │   └── storage-service
│   ├── bff/               # Backend for Frontend
│   └── web/               # Frontend application
├── libs/                  # Shared libraries (future use)
├── platform/              # Infrastructure configuration
│   └── docker/
│       ├── demo/
│       └── local/
└── docs/                  # Documentation
    ├── adr/               # Architecture Decision Records
    └── legacy-docs/       # Legacy documentation
```

## Hybrid Hexagonal Architecture

Each service follows the **Hybrid Hexagonal Architecture** pattern, which combines the benefits of traditional Hexagonal (Ports & Adapters) architecture with the pragmatic approach of Spring Boot layered architecture.

### Package Structure

```
com.demo.ec.[service]/
├── web/                   # Inbound Adapter (REST Controllers, DTOs)
├── application/           # Use Cases (Business flow control, Transaction boundaries)
├── domain/                # Core Domain (Entities, Value Objects, Domain Services)
├── gateway/               # Outbound Adapters
│   ├── [repository]/      # Database access (MyBatis/JPA)
│   ├── client/            # External service clients
│   ├── messaging/         # Kafka producers/consumers
│   └── [other adapters]/  # Other external integrations
└── config/                # Configuration classes
```

### Layer Responsibilities

#### 1. Web Layer (`web/`)
- **Role**: Inbound Adapter
- **Contents**: REST Controllers, Request/Response DTOs
- **Dependencies**: Can depend on `application` and `domain`
- **Example**: `StorageController`, `OrderController`

#### 2. Application Layer (`application/`)
- **Role**: Use Case orchestration
- **Contents**: Service interfaces and implementations, business flow control
- **Responsibilities**: 
  - Transaction boundaries
  - Orchestrating domain logic
  - Coordinating with gateways
- **Dependencies**: Can depend on `domain` and `gateway` interfaces
- **Example**: `OrderATService`, `StorageATServiceImpl`

#### 3. Domain Layer (`domain/`)
- **Role**: Core business logic
- **Contents**: Entities, Value Objects, Domain Services, Enums
- **Characteristics**: 
  - **Zero external dependencies** (pure Java)
  - Framework-agnostic
  - Contains business rules and invariants
- **Example**: `Order`, `Storage`, `OrderStatus`

#### 4. Gateway Layer (`gateway/`)
- **Role**: Outbound Adapters
- **Contents**: 
  - Repository implementations (MyBatis Mappers)
  - External service clients (Feign, RestClient)
  - Messaging adapters (Kafka producers/consumers)
  - Other external integrations (Elasticsearch, MongoDB, MinIO)
- **Dependencies**: Can depend on `domain`
- **Example**: `StorageMapper`, `OrderServiceClient`, `OrderEventPublisher`

#### 5. Config Layer (`config/`)
- **Role**: Spring configuration
- **Contents**: Configuration classes, Bean definitions
- **Example**: `RedisConfig`, `KafkaConfig`, `RestClientConfig`

## Package Migration

All services have been migrated from the old package structure to the new one:

### Old Package Structure
```
com.example.seata.at.[service]/
├── api/                   # Controllers
├── service/               # Business logic
├── domain/
│   ├── entity/
│   └── mapper/            # MyBatis mappers
└── config/
```

### New Package Structure
```
com.demo.ec.[service]/
├── web/                   # Controllers (from api/)
├── application/           # Business logic (from service/)
├── domain/                # Entities (from domain/entity/)
├── gateway/               # Mappers and external adapters (from domain/mapper/)
└── config/                # Configuration
```

## Key Architectural Principles

### 1. Dependency Rule
- **Inward dependencies only**: Outer layers can depend on inner layers, but not vice versa
- **Domain independence**: The `domain` layer has zero external dependencies
- **Interface segregation**: Use interfaces (Ports) to decouple layers

### 2. Separation of Concerns
- **Web layer**: HTTP concerns only (request/response handling)
- **Application layer**: Business flow orchestration
- **Domain layer**: Pure business logic
- **Gateway layer**: External system integration

### 3. Testability
- **Domain layer**: Unit testable without any framework
- **Application layer**: Testable with mocked gateways
- **Gateway layer**: Integration testable

## Benefits of Hybrid Hexagonal

### vs Traditional Layered Architecture
- **Better isolation**: Domain layer is completely isolated from infrastructure concerns
- **Easier testing**: Pure domain logic can be tested without frameworks
- **Flexibility**: Easy to swap out adapters (e.g., change from MyBatis to JPA)

### vs Pure Hexagonal Architecture
- **Pragmatic**: Avoids overly deep package hierarchies like `adapter/in/web`
- **Spring Boot friendly**: Aligns with Spring Boot conventions
- **Developer friendly**: Easier to navigate and understand

## Migration Summary

### Services Migrated
1. ✅ **storage-service**: `com.example.seata.at.storage` → `com.demo.ec.storage`
2. ✅ **account-service**: `com.example.seata.at.account` → `com.demo.ec.account`
3. ✅ **order-service**: `com.example.seata.at.order` → `com.demo.ec.order`
4. ✅ **payment-service**: Restructured to Hybrid Hexagonal (already used `com.demo.ec`)
5. ✅ **alert-service**: Restructured to Hybrid Hexagonal (already used `com.demo.ec`)
6. ✅ **es-service**: Restructured to Hybrid Hexagonal (already used `com.demo.ec`)
7. ✅ **bff**: Restructured to Hybrid Hexagonal (already used `com.demo.ec`)

### Infrastructure Changes
- ✅ Moved services to `apps/services/`
- ✅ Moved BFF to `apps/bff/`
- ✅ Moved frontend to `apps/web/`
- ✅ Moved Docker configs to `platform/docker/`
- ✅ Organized documentation in `docs/`
- ✅ Updated root `pom.xml` with new module paths
- ✅ Updated scripts (`back.sh`, `front.sh`, `init.sh`)

## Next Steps

1. **Shared Libraries**: Create common libraries in `libs/` for shared DTOs, utilities, etc.
2. **API Gateway**: Consider adding an API Gateway in `apps/gateway/`
3. **Service Mesh**: Evaluate service mesh for inter-service communication
4. **Documentation**: Continue documenting architectural decisions in `docs/adr/`

## References

- [ADR-001: Adoption of Hybrid Hexagonal Monorepo](docs/adr/001-adoption-of-hybrid-hexagonal-monorepo.md)
- [Design Standards](docs/legacy-docs/guide/design-standards.md)
- [Architecture README](docs/legacy-docs/architecture/README_ARCHITECTURE.md)
