# Repository Guidelines

## Project Structure & Module Organization
- `apps/bff`: Backend-for-Frontend (REST/WebSocket, auth/session handling).
- `apps/services/*`: Domain microservices (`order-service`, `payment-service`, `storage-service`, `account-service`, `alert-service`, `es-service`).
- `apps/web`: Vue 3 + TypeScript frontend (Vite).
- `platform/docker/local`: local middleware stack (MySQL/Redis/Seata; optional Kafka/Elasticsearch).
- `platform/docker/demo`: VPS/demo deployment compose files.
- `docs/adr`, `docs/architecture`, `docs/runbook`: architecture decisions and operational docs.
- `scripts/`: flow and smoke-test utilities (for example, `scripts/test-saga.sh`).

## Build, Test, and Development Commands
- `cd platform/docker/local && docker compose up -d`: start required local middleware.
- `docker compose --profile kafka --profile elastic up -d`: add optional Kafka/Elasticsearch.
- `mvn clean package -DskipTests`: build all backend modules.
- `mvn test`: run backend tests across modules.
- `mvn spring-boot:run -pl apps/bff`: run a single service (swap module path as needed).
- `cd apps/web && pnpm install && pnpm dev`: run frontend in development.
- `cd apps/web && pnpm build`: build frontend production assets.
- `./scripts/test-saga.sh 1 1`: quick order/Saga flow smoke check.

## Coding Style & Naming Conventions
- Java baseline is 21; keep service code aligned with Hybrid Hexagonal layering: `web -> application -> domain <- gateway`.
- Do not place Spring/DB framework annotations or infrastructure logic in `domain`.
- Follow Google Java Style (4-space indentation, clear class names).
- Frontend uses TypeScript + Vue SFCs with 2-space indentation; use `PascalCase` component/view file names (for example, `CheckoutView.vue`).
- Name tests with suffixes like `*Test`, `*IntegrationTest`, or `*UnitTest`.

## Testing Guidelines
- Backend tests use Spring Boot Test (JUnit 5), with RestAssured and `spring-kafka-test` where needed.
- Run all tests with `mvn test`; run module tests with `mvn test -pl apps/services/order-service`.
- For Saga/payment changes, cover happy path, idempotency, and compensation/failure paths.
- No enforced coverage gate is defined; new behavior should include regression tests.

## Commit & Pull Request Guidelines
- History follows mostly Conventional Commit prefixes: `feat:`, `fix:`, `refactor:` (scopes like `feat(search): ...` are encouraged).
- Keep commits focused to one logical change and mention the affected module.
- PRs should include: summary, impacted services, local verification commands/results, config/env updates, and UI screenshots for frontend changes.
- Link related issue/ADR when changing architecture or cross-service contracts.

## Security & Configuration Tips
- Never commit secrets from `.env` or `apps/bff/src/main/resources/serviceAccountKey.json`.
- Use `./init.sh pull` to sync local env files, then verify `/actuator/health` endpoints before running integration flows.
