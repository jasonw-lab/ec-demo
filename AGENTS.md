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

## Docker Environment Configuration Rules

### BASEPATH Rule
- All middleware config files must be placed under `BASEPATH`, NOT in the repository
- Default BASEPATH: `/Users/wangjw/Dev/_Env/_demo/seata-mode`
- Environment variable: `export BASEPATH=/path/to/your/env`

### Config File Management
1. Source config templates are stored in `platform/docker/demo/conf/`
2. If config does not exist in BASEPATH, copy from source
3. Never commit runtime data or environment-specific configs to the repository

### BASEPATH Directory Structure
```
${BASEPATH}/
  mysql/
    conf/my.cnf          # MySQL configuration
    data/                # MySQL data files
    log/                 # MySQL logs
  seata/
    conf/application.yml # Seata server configuration
    logs/                # Seata logs
  redis/
    conf/redis.conf      # Redis configuration
    data/                # Redis persistence
  kafka/
    data/                # Kafka data
  elasticsearch/
    data/                # Elasticsearch indices
  mongodb/
    data/                # MongoDB data
  minio/
    data/                # MinIO object storage
```

### Setup Process
```bash
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode
./setup-env.sh  # Creates directories and copies configs if missing
docker compose -f docker-compose-demo-env.yml up -d
```

### When Adding New Middleware
1. Add source config to `platform/docker/demo/conf/<middleware>/conf/`
2. Update `setup-env.sh` to copy the new config
3. Update `docker-compose-demo-env.yml` with `${BASEPATH}/<middleware>/conf/` volume mount
4. Update this document with the new directory structure

## Docker Service Port Mapping Rules

### Application Service Ports (Internal)
| Service | Internal Port | External Port | Environment Variable |
|---------|--------------|---------------|---------------------|
| BFF | 8080 | 18080 | - |
| order-service | 8082 | 18081 | `ORDER_SERVICE_BASE_URL` |
| storage-service | 8083 | 18082 | `STORAGE_SERVICE_BASE_URL` |
| account-service | 8083 | 18083 | `ACCOUNT_SERVICE_BASE_URL` |
| payment-service | 8084 | 18090 | `PAYMENT_SERVICE_BASE_URL` |
| es-service | 8086 | 8086 | `ES_SERVICE_BASE_URL` |

### Service URL Configuration
- Docker コンテナ間通信は**内部ポート**を使用
- ホストからのアクセスは**外部ポート**を使用
- 環境変数例: `ORDER_SERVICE_BASE_URL=http://ec-demo-order-service:8082`

### Common Mistakes to Avoid
1. ❌ `localhost` をコンテナ間通信に使用しない（コンテナ名を使用）
2. ❌ 外部ポートをコンテナ間通信に使用しない（内部ポートを使用）
3. ❌ application.yml のデフォルトポートと docker-compose のポートマッピングの不一致

### nginx Proxy Configuration
- `/ec-api/*` → `ec-demo-bff:8080` (BFF API)
- `/ec-api/ws/*` → `ec-demo-bff:8080/ws/` (WebSocket)
- `/ec-demo/*` → 静的ファイル (フロントエンド)

## Elasticsearch & MinIO Initialization

### ES Index Setup
```bash
cd platform/docker/demo/elasticsearch
./init-es-products-index.sh
```
- インデックス `products_v1` 作成
- エイリアス `products` 設定
- CSV からプロダクトデータインポート

### MinIO Image Upload
```bash
cd platform/docker/demo/elasticsearch
./init-upload-product-minio.sh
```
- バケット `ec-demo` 作成（public アクセス）
- `product/images/` に画像アップロード

### Full Demo Environment Setup Order
1. `./setup-env.sh` - 設定ファイルコピー
2. `docker compose -f docker-compose-demo-env.yml --profile kafka --profile elastic --profile mongo --profile minio up -d` - ミドルウェア起動
3. `./init-es-products-index.sh` - ES インデックス作成
4. `./init-upload-product-minio.sh` - MinIO 画像アップロード
5. `docker compose -f docker-compose-demo-app.yml --profile elastic up -d` - アプリ起動
