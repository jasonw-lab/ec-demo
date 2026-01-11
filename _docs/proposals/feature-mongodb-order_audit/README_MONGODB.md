# MongoDB セットアップ手順

## 認証情報

| 項目 | 値 | 環境変数 |
|------|-----|----------|
| Root ユーザー | `admin` | `MONGO_ROOT_USER` |
| Root パスワード | `admin123` | `MONGO_ROOT_PASSWORD` |
| データベース | `ec_demo` | - |
| ポート | `27017` | - |

## 接続URI

```bash
# ローカル開発環境（デフォルト）
mongodb://admin:admin123@localhost:27017/ec_demo?authSource=admin

# リモートDockerサーバー（EC_DEMO_SERVER使用）
mongodb://admin:admin123@192.168.1.199:27017/ec_demo?authSource=admin

# Docker内部ネットワーク（コンテナ間通信）
mongodb://admin:admin123@ec-demo-mongodb:27017/ec_demo?authSource=admin
```

## 接続方法の優先順位

アプリケーションは以下の優先順位でMongoDB接続先を決定します:

1. **MONGODB_URL** (最優先) - 完全なURL指定
2. **EC_DEMO_SERVER** - .envで設定された場合、そのホストに接続
3. **localhost** (デフォルト) - 上記が未設定の場合

## 初期化内容

初期化スクリプト: [`sql/mongodb-init/init-mongo.js`](../../docker/demo/sql/mongodb-init/init-mongo.js)

### 作成されるコレクション
- `order_audit`: 注文監査ログ

### インデックス
| フィールド | タイプ | 説明 |
|-----------|--------|------|
| `orderId` | unique | 注文ID（主キー） |
| `processedEventIds` | normal | 冪等性チェック用eventID配列 |
| `history.eventId` | normal | 履歴内eventID検索用 |
| `currentStatus` | normal | 現在ステータス検索用 |
| `createdAt` | desc | 作成日時降順（最新順） |

## アプリケーション設定

### application.yaml
```yaml
spring:
  data:
    mongodb:
      # 優先順位: MONGODB_URL > EC_DEMO_SERVER > localhost
      uri: ${MONGODB_URL:mongodb://${MONGO_USER:admin}:${MONGO_PASSWORD:admin123}@${EC_DEMO_SERVER:localhost}:27017/ec_demo?authSource=admin}
```

### 環境変数での上書き

#### パターン1: EC_DEMO_SERVERを使用（.envで設定）
```bash
# order-service/.env
EC_DEMO_SERVER=192.168.1.199
# → mongodb://admin:admin123@192.168.1.199:27017/ec_demo?authSource=admin
```

#### パターン2: MONGODB_URLで完全指定（最優先）
```bash
# Docker Compose環境など
export MONGODB_URL="mongodb://admin:admin123@ec-demo-mongodb:27017/ec_demo?authSource=admin"
```

#### パターン3: デフォルト（localhost）
```bash
# 環境変数なし
# → mongodb://admin:admin123@localhost:27017/ec_demo?authSource=admin
```

## 動作確認

### 1. MongoDB起動
```bash
cd _docs/docker/demo
docker-compose -f docker-compose-demo-env.yml up -d ec-demo-mongodb
```

### 2. 接続確認
```bash
docker exec -it ec-demo-mongodb-6 mongosh -u admin -p admin123 --authenticationDatabase admin
```

### 3. データ確認
```javascript
use ec_demo
db.order_audit.find().pretty()
db.order_audit.getIndexes()
```

## トラブルシューティング

### 認証エラー
```
MongoServerError: Authentication failed
```
→ `authSource=admin` パラメータを確認してください

### 接続タイムアウト
```
MongoTimeoutException: Timed out after 30000 ms
```
→ MongoDB起動確認: `docker ps | grep mongodb`
→ ヘルスチェック確認: `docker inspect ec-demo-mongodb-6 | grep Health`
