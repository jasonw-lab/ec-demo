// ============================================================
// MongoDB 初期化スクリプト for ec-demo
// ============================================================
// このスクリプトはコンテナ起動時に自動実行されます
// 実行タイミング: docker-entrypoint-initdb.d/ 配下のスクリプトとして
// 実行ユーザー: root (MONGO_INITDB_ROOT_USERNAME)
// ============================================================

print('🚀 Starting MongoDB initialization for ec_demo...');

// ec_demo データベースに切り替え
db = db.getSiblingDB('ec_demo');

print('📁 Creating collections...');

// ============================================================
// 1. order_audit コレクション（注文監査ログ）
// ============================================================
try {
    db.createCollection('order_audit', {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["orderId", "currentStatus", "processedEventIds", "history", "createdAt", "updatedAt"],
                properties: {
                    orderId: {
                        bsonType: "string",
                        description: "注文ID（主キー）"
                    },
                    currentStatus: {
                        bsonType: "string",
                        enum: ["CREATED", "PENDING", "PAYMENT_PENDING", "PROCESSING", "PAID", "CANCELLED", "COMPLETED"],
                        description: "現在のステータス"
                    },
                    processedEventIds: {
                        bsonType: "array",
                        items: { bsonType: "string" },
                        description: "処理済みeventID配列（冪等性保証用）"
                    },
                    history: {
                        bsonType: "array",
                        description: "ステータス変更履歴",
                        items: {
                            bsonType: "object",
                            required: ["status", "at", "by", "eventId"],
                            properties: {
                                status: { bsonType: "string" },
                                reason: { bsonType: "string" },
                                at: { bsonType: "date" },
                                by: { bsonType: "string" },
                                eventId: { bsonType: "string" },
                                metadata: { bsonType: "object" }
                            }
                        }
                    },
                    createdAt: { bsonType: "date" },
                    updatedAt: { bsonType: "date" }
                }
            }
        }
    });
    print('  ✅ order_audit collection created with schema validation');
} catch (e) {
    print('  ⚠️  order_audit collection already exists or error:', e.message);
}

// ============================================================
// 2. インデックス作成
// ============================================================
print('📑 Creating indexes...');

// 2.1 orderId（主キー、ユニーク）
db.order_audit.createIndex(
    { "orderId": 1 }, 
    { unique: true, name: "idx_orderId_unique" }
);
print('  ✅ Index: orderId (unique)');

// 2.2 processedEventIds（冪等性チェック用）
db.order_audit.createIndex(
    { "processedEventIds": 1 }, 
    { name: "idx_processedEventIds" }
);
print('  ✅ Index: processedEventIds');

// 2.3 history.eventId（履歴内eventID検索用）
db.order_audit.createIndex(
    { "history.eventId": 1 }, 
    { name: "idx_history_eventId" }
);
print('  ✅ Index: history.eventId');

// 2.4 currentStatus（ステータス検索用）
db.order_audit.createIndex(
    { "currentStatus": 1 }, 
    { name: "idx_currentStatus" }
);
print('  ✅ Index: currentStatus');

// 2.5 createdAt（時系列検索用、降順）
db.order_audit.createIndex(
    { "createdAt": -1 }, 
    { name: "idx_createdAt_desc" }
);
print('  ✅ Index: createdAt (desc)');

// 2.6 複合インデックス: currentStatus + createdAt（よく使うクエリパターン用）
db.order_audit.createIndex(
    { "currentStatus": 1, "createdAt": -1 }, 
    { name: "idx_status_createdAt" }
);
print('  ✅ Index: currentStatus + createdAt');

// ============================================================
// 3. サンプルデータ投入（開発環境のみ）
// ============================================================
print('📊 Inserting sample data...');

const sampleData = {
    orderId: "ORD-SAMPLE-001",
    currentStatus: "COMPLETED",
    processedEventIds: ["evt-001", "evt-002", "evt-003"],
    history: [
        {
            status: "CREATED",
            at: new Date("2026-01-11T10:00:00Z"),
            by: "order-svc",
            eventId: "evt-001",
            metadata: { sourceEvent: "OrderCreated" }
        },
        {
            status: "PAID",
            at: new Date("2026-01-11T10:05:00Z"),
            by: "order-svc",
            eventId: "evt-002",
            metadata: { sourceEvent: "PaymentCompleted", paymentStatus: "SUCCESS" }
        },
        {
            status: "COMPLETED",
            at: new Date("2026-01-11T10:10:00Z"),
            by: "order-svc",
            eventId: "evt-003",
            metadata: { sourceEvent: "OrderCompleted" }
        }
    ],
    createdAt: new Date("2026-01-11T10:00:00Z"),
    updatedAt: new Date("2026-01-11T10:10:00Z")
};

try {
    db.order_audit.insertOne(sampleData);
    print('  ✅ Sample data inserted: ' + sampleData.orderId);
} catch (e) {
    print('  ⚠️  Sample data already exists or error:', e.message);
}

// ============================================================
// 4. 初期化結果の確認
// ============================================================
print('\n📋 Initialization Summary:');
print('  Database: ec_demo');
print('  Collections: ' + db.getCollectionNames().length);
print('  Indexes on order_audit: ' + db.order_audit.getIndexes().length);
print('  Documents in order_audit: ' + db.order_audit.countDocuments());

print('\n✅ MongoDB initialization completed successfully!\n');
