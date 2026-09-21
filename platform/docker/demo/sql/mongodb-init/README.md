# MongoDB 初期化スクリプト 実行ガイド

## 📁 スクリプト構成

```
sql/mongodb-init/
├── init-mongo.js          # メイン初期化スクリプト
└── README.md              # このファイル
```

## 🚀 実行方法

### 方法1: Docker Compose による自動実行（推奨）

**これが最も簡単で推奨される方法です。**

#### ステップ1: Docker Compose 起動

```bash
cd /Users/{user-name}/Dev/Git/ross-dev2024/ec-demo-dev/feature-kafka-alert/_docs/docker/demo

# MongoDB を起動（初回起動時に自動で初期化スクリプトが実行される）
docker-compose -f docker-compose-demo-env.yml up -d ec-demo-mongodb
```

#### ステップ2: 初期化完了の確認

```bash
# ログを確認
docker logs ec-demo-mongodb-6 2>&1 | grep "MongoDB initialization"

# 期待される出力:
# 🚀 Starting MongoDB initialization for ec_demo...
# ✅ MongoDB initialization completed successfully!
```

#### ステップ3: データ確認

```bash
# MongoDBに接続
docker exec -it ec-demo-mongodb-6 mongosh -u admin -p admin123 --authenticationDatabase admin

# MongoDB Shell内で実行
use ec_demo
db.order_audit.find().pretty()
db.order_audit.getIndexes()
```

---

### 方法2: 手動実行（既存のMongoDBコンテナに対して）

すでにMongoDBコンテナが起動している場合、手動でスクリプトを実行できます。

```bash
# スクリプトをコンテナにコピー
docker cp sql/mongodb-init/init-mongo.js ec-demo-mongodb-6:/tmp/

# MongoDBコンテナ内でスクリプト実行
docker exec -it ec-demo-mongodb-6 mongosh \
  -u admin \
  -p admin123 \
  --authenticationDatabase admin \
  /tmp/init-mongo.js
```

---

### 方法3: ローカルMongoDBで実行

ローカルにMongoDBがインストールされている場合：

```bash
cd _docs/docker/demo

# 認証情報を指定して実行
mongosh -u admin -p admin123 --authenticationDatabase admin < sql/mongodb-init/init-mongo.js

# または接続後にload()で実行
mongosh -u admin -p admin123 --authenticationDatabase admin
> load('/path/to/sql/mongodb-init/init-mongo.js')
```

---

## 🔄 再初期化（リセット）

初期化をやり直したい場合：

### オプション1: コレクションのみ削除

```bash
docker exec -it ec-demo-mongodb-6 mongosh -u admin -p admin123 --authenticationDatabase admin

# MongoDB Shell内で
use ec_demo
db.order_audit.drop()
exit

# スクリプトを再実行
docker exec -it ec-demo-mongodb-6 mongosh \
  -u admin -p admin123 \
  --authenticationDatabase admin \
  /tmp/init-mongo.js
```

### オプション2: データベース全体を削除

```bash
docker exec -it ec-demo-mongodb-6 mongosh -u admin -p admin123 --authenticationDatabase admin

# MongoDB Shell内で
use ec_demo
db.dropDatabase()
exit
```

### オプション3: コンテナとボリュームを完全削除

```bash
cd _docs/docker/demo

# コンテナ停止と削除
docker-compose -f docker-compose-demo-env.yml down ec-demo-mongodb

# ボリュームも削除（データが完全に消えます！）
docker volume rm $(docker volume ls -q | grep mongodb)

# または docker-compose.yml で定義されているボリュームパスを削除
rm -rf /mydata/ec-demo/mongodb/data

# 再起動（初期化スクリプトが再実行される）
docker-compose -f docker-compose-demo-env.yml up -d ec-demo-mongodb
```

---

## 📊 初期化スクリプトの内容

### 作成されるもの

#### 1. コレクション
- **order_audit**: 注文監査ログ（スキーマバリデーション付き）

#### 2. インデックス（6個）

| インデックス名 | フィールド | タイプ | 用途 |
|--------------|-----------|--------|------|
| idx_orderId_unique | orderId | unique | 主キー |
| idx_processedEventIds | processedEventIds | normal | 冪等性チェック |
| idx_history_eventId | history.eventId | normal | 履歴内eventID検索 |
| idx_currentStatus | currentStatus | normal | ステータス検索 |
| idx_createdAt_desc | createdAt | desc | 時系列検索 |
| idx_status_createdAt | currentStatus + createdAt | compound | ステータス×時系列 |

#### 3. サンプルデータ
- サンプル注文: `ORD-SAMPLE-001`（CREATED → PAID → COMPLETED）

### スキーマバリデーション

```javascript
{
  orderId: string (required),
  currentStatus: enum ["CREATED", "PENDING", "PROCESSING", "PAID", "CANCELLED", "COMPLETED"],
  processedEventIds: array<string>,
  history: array<{
    status: string,
    reason: string?,
    at: date,
    by: string,
    eventId: string,
    metadata: object?
  }>,
  createdAt: date,
  updatedAt: date
}
```

---

## ✅ 動作確認コマンド

### 1. コレクション確認
```javascript
use ec_demo
show collections
// 期待される出力: order_audit
```

### 2. インデックス確認
```javascript
db.order_audit.getIndexes()
// 7個のインデックス（_id含む）が表示される
```

### 3. サンプルデータ確認
```javascript
db.order_audit.findOne({ orderId: "ORD-SAMPLE-001" })
```

### 4. バリデーションテスト
```javascript
// ❌ 失敗するべき（必須フィールド不足）
db.order_audit.insertOne({ orderId: "TEST" })

// ✅ 成功するべき
db.order_audit.insertOne({
  orderId: "ORD-TEST-001",
  currentStatus: "CREATED",
  processedEventIds: [],
  history: [],
  createdAt: new Date(),
  updatedAt: new Date()
})
```

---

## 🐛 トラブルシューティング

### スクリプトが実行されない

**原因**: `docker-entrypoint-initdb.d/` はデータベースが空の時のみ実行されます。

**解決策**:
```bash
# データを削除して再起動
docker-compose -f docker-compose-demo-env.yml down -v
docker-compose -f docker-compose-demo-env.yml up -d ec-demo-mongodb
```

### 認証エラー

```
MongoServerError: Authentication failed
```

**解決策**: `authSource=admin` を指定
```bash
mongosh -u admin -p admin123 --authenticationDatabase admin
```

### 接続できない

```
MongoNetworkError: connect ECONNREFUSED
```

**解決策**: コンテナの起動状態を確認
```bash
docker ps | grep mongodb
docker logs ec-demo-mongodb-6
```

---

## 📚 関連ドキュメント

- [README_MONGODB.md](../README_MONGODB.md): MongoDB全体のセットアップガイド
- [docker-compose-demo-env.yml](../docker-compose-demo-env.yml): Docker Compose設定
- [application.yaml](../../../../apps/services/order-service/src/main/resources/application.yaml): Spring Boot接続設定
