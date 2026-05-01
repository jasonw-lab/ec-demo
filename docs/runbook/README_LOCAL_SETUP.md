# ローカル起動手順（確定版）

このリポジトリは「VPSデプロイ想定のCompose」と「開発端末向けのローカルCompose」を分けて運用します。

## 1. 事前準備（環境ファイル）

- BFF/バックエンド: `.env`（PayPay設定）
- フロント: `apps/web/.env`（Vite + Firebase）
- Firebase Admin: `apps/bff/src/main/resources/serviceAccountKey.json`

既存の環境から取得する場合（任意）:

```bash
./init.sh pull
```

## 2. ローカル向け（推奨）: ミドルウェアのみをComposeで起動

アプリ（apps/bff / apps/services/* / apps/web）はローカルで起動し、依存ミドルウェアをComposeで揃える構成です。

### 2.1 起動

```bash
cd platform/docker/local
docker compose up -d
```

### 2.2 追加（任意）

Kafka / Elasticsearch は重いので任意にしています。

```bash
cd platform/docker/local
docker compose --profile kafka --profile elastic up -d
```

### 2.3 ポート一覧（ローカルCompose）

| ミドルウェア | ポート | 備考 |
|---|---:|---|
| MySQL | 3307 | init SQL 自動実行 |
| Seata Console | 7092 | 管理UI |
| Seata Server | 8092 | Saga Coordinator |
| Redis | 6379 | BFFセッション |
| Kafka | 9092 | `--profile kafka` |
| Elasticsearch | 9200 | `--profile elastic` |

### 2.4 ヘルスチェック（例）

- MySQL: `docker logs ec-demo-mysql-8` / `docker exec -it ec-demo-mysql-8 mysql -uroot -p123456 -e "SHOW DATABASES;"`
- Seata Console: `http://localhost:7092`
- order-service: `http://localhost:8082/actuator/health`
- storage-service: `http://localhost:8083/actuator/health`
- account-service: `http://localhost:8081/actuator/health`
- payment-service: `http://localhost:8084/actuator/health`
- alert-service: `http://localhost:8085/actuator/health`

### 2.5 初期データ投入

MySQLコンテナ初回起動時に `platform/docker/demo/sql/mysql-init` 配下のSQLを自動実行します。

再投入したい場合は、MySQLのデータボリュームを消してから再起動します（環境により手順が異なるため、ローカルのDockerボリューム管理ポリシーに従ってください）。

## 3. デモ環境向けCompose（全コンテナ起動）

`platform/docker/demo` はミドルウェア + アプリを全てComposeで起動する構成です。

### 3.1 事前準備

環境変数 `BASEPATH` を設定し、設定ファイルをコピーします。

```bash
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode
./setup-env.sh
```

### 3.2 ミドルウェア起動

```bash
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode

# 基本（MySQL, Seata, Redis, nginx）
docker compose -f docker-compose-demo-env.yml up -d

# Kafka追加
docker compose -f docker-compose-demo-env.yml --profile kafka up -d

# Elasticsearch追加
docker compose -f docker-compose-demo-env.yml --profile elastic up -d

# MongoDB追加
docker compose -f docker-compose-demo-env.yml --profile mongo up -d

# 全プロファイル
docker compose -f docker-compose-demo-env.yml --profile kafka --profile elastic --profile mongo up -d
```

### 3.3 アプリケーション起動

```bash
cd platform/docker/demo
export BASEPATH=/Users/wangjw/Dev/_Env/_demo/seata-mode

# 基本サービス（account, storage, order, payment, bff）
docker compose -f docker-compose-demo-app.yml up -d

# es-service追加（Elasticsearch連携）
docker compose -f docker-compose-demo-app.yml --profile elastic up -d
```

### 3.4 ポート一覧（デモCompose）

#### ミドルウェア

| サービス | ホストポート | コンテナポート | 備考 |
|---|---:|---:|---|
| nginx | 80, 443 | 80, 443 | リバースプロキシ |
| MySQL | 3307 | 3306 | init SQL 自動実行 |
| Seata Console | 7092 | 7091 | 管理UI |
| Seata Server | 8091 | 8091 | Saga Coordinator |
| Redis | 6379 | 6379 | BFFセッション |
| Kafka | 29092 | 29092 | `--profile kafka` |
| Kafka UI | 8090 | 8080 | `--profile kafka` |
| Elasticsearch | 9200, 9300 | 9200, 9300 | `--profile elastic` |
| Kibana | 5601 | 5601 | `--profile elastic` |
| MongoDB | 27017 | 27017 | `--profile mongo` |

#### アプリケーション

| サービス | ホストポート | コンテナポート | 備考 |
|---|---:|---:|---|
| BFF | 18080 | 8080 | API Gateway |
| account-service | 18083 | 8083 | |
| storage-service | 18082 | 8083 | |
| order-service | 18081 | 8082 | |
| payment-service | 18090 | 8084 | (外部ポート8090は旧設定) |
| es-service | 8086 | 8086 | `--profile elastic` |

### 3.5 nginx経由のアクセス

| パス | 転送先 | 備考 |
|---|---|---|
| `/ec-api/*` | ec-demo-bff:8080 | BFF API |
| `/ec-api/ws/*` | ec-demo-bff:8080/ws/ | WebSocket |
| `/ec-demo/*` | 静的ファイル | フロントエンド |

### 3.6 ヘルスチェック

```bash
# ミドルウェア
curl http://localhost:9200/_cluster/health  # Elasticsearch
curl http://localhost:7092                   # Seata Console

# アプリケーション（nginx経由）
curl http://localhost/ec-api/actuator/health

# アプリケーション（直接）
curl http://localhost:18080/actuator/health  # BFF
curl http://localhost:18081/actuator/health  # order-service (内部8082)
curl http://localhost:18082/actuator/health  # storage-service (内部8083)
curl http://localhost:18083/actuator/health  # account-service (内部8083)
```

### 3.7 環境設定ファイル

BASEPATH配下に以下の構成で設定ファイルを配置:

```
${BASEPATH}/
  mysql/conf/my.cnf
  seata/conf/application.yml
  redis/conf/redis.conf
  nginx/conf/nginx.conf
  nginx/conf/conf.d/default.conf
  nginx/conf/conf.d/sub/ec-demo.conf
  nginx/html/index.html
  nginx/html/50x.html
  env/ec-demo-bff.env
  env/ec-demo-payment-service.env
```

## 4. VPS/本番デプロイ（参考）

`platform/docker/demo` は以下を前提にしたデプロイ用です。

- `jason-lab-net` という外部ネットワークが存在する
- `BASEPATH` 配下へのボリュームマウントが前提
- BFFの `env_file` はホスト側の `${BASEPATH}/env/ec-demo-bff.env` を参照する

詳細: `platform/docker/demo/deploy.md`
