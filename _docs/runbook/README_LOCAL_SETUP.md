# ローカル起動手順（確定版）

このリポジトリは「VPSデプロイ想定のCompose」と「開発端末向けのローカルCompose」を分けて運用します。

## 1. 事前準備（環境ファイル）

- BFF/バックエンド: `.env`（PayPay設定）
- フロント: `front/.env`（Vite + Firebase）
- Firebase Admin: `bff/src/main/resources/serviceAccountKey.json`

既存の環境から取得する場合（任意）:

```bash
./init.sh pull
```

## 2. ローカル向け（推奨）: ミドルウェアのみをComposeで起動

アプリ（BFF / order / storage / account / front）はローカルで起動し、依存ミドルウェアをComposeで揃える構成です。

### 2.1 起動

```bash
cd _docs/docker/local
docker compose up -d
```

### 2.2 追加（任意）

Kafka / Elasticsearch は重いので任意にしています。

```bash
cd _docs/docker/local
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

MySQLコンテナ初回起動時に `_docs/docker/demo/sql/mysql-init` 配下のSQLを自動実行します。

再投入したい場合は、MySQLのデータボリュームを消してから再起動します（環境により手順が異なるため、ローカルのDockerボリューム管理ポリシーに従ってください）。

## 3. VPS/デモ環境向けCompose（参考）

`_docs/docker/demo` は以下を前提にしたデプロイ用です。

- `docker_youlai-boot` という外部ネットワークが存在する
- `/mydata` 配下へのボリュームマウントが前提
- BFFの `env_file` はホスト側の `/mydata/ec-demo/env/ec-demo-bff.env` を参照する

詳細: `_docs/docker/demo/deploy.md`
