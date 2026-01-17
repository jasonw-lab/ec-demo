# ec-demo MongoDB 要件定義（order_audit）

## 1. ユースケース概要
本要件は、ec-demo において **注文ライフサイクル監査ログ（Order Timeline / Audit）** を実現するためのものである。

- 注文の現在状態は RDB（MySQL 等）で管理する  
- 注文の状態遷移履歴（証跡・監査ログ）は MongoDB で管理する  
- MongoDB は「履歴・説明責任」のために限定利用する  

---

## 2. 目的（Why）
- 注文が **なぜ・いつ・どのように** 現在の状態に至ったかを後から説明可能にする
- 障害調査、CS 対応、監査対応を想定した設計とする
- RDB の正データと履歴データを明確に分離する

---

## 3. ユースケース（What）
- 注文作成、決済成功/失敗、在庫引当、キャンセルなど  
  **注文状態が変化するたびに、その履歴を MongoDB に追記保存する**
- 特定の注文IDを指定して、状態遷移のタイムラインを取得できる

---

## 4. 対象外（Non-goals）
- MongoDB を注文の正データとして使用しない
- 高度な分析・検索機能は本要件では扱わない
- MongoDB トランザクションは使用しない

---

## 5. データ要件（Data Requirements）

### 5.1 コレクション
- コレクション名：`order_audit`（命名は既存規約に合わせて調整可）

### 5.2 データモデル
- **1注文 = 1ドキュメント**
- ドキュメント内に **状態遷移の履歴配列（history）** を保持する
- 主キーは `orderId`（`_id = orderId` としてもよい）

### 5.3 サンプルデータ
```json
{
  "orderId": "ORD-1001",
  "currentStatus": "PAID",
  "history": [
    {
      "status": "CREATED",  // CREATED, PAID, CANCELLED のみ（理由は reason で区別）
      "reason": null,  // CANCELLED時のみ: "PAYMENT_FAILED", "TIMEOUT", "USER_CANCEL" 等
      "at": "2026-01-01T10:01:00Z",
      "by": "order-svc",
      "eventId": "evt-ord-1001-created",
      "metadata": {
        "userId": "USR-001",
        "totalAmount": 15000
      }
    },
    {
      "status": "PAID",
      "reason": null,
      "at": "2026-01-01T10:03:00Z",
      "by": "order-svc",
      "eventId": "evt-pay-123",
      "metadata": {
        "sourceEvent": "PaymentCompleted",
        "sourceService": "payment-svc",
        "paymentId": "PAY-456"
      }
    }
  ],
  "createdAt": "2026-01-01T10:01:00Z",
  "updatedAt": "2026-01-01T10:03:00Z"
}
```

**設計意図**:  
- `by` は常に "order-svc"（書き込み責務の一元化）
- 元のイベント発行元は `metadata.sourceService` に記録
- `currentStatus` はクエリ最適化のための非正規化フィールド

### 5.4 必須フィールド
- orderId：注文ID
- currentStatus：現在の注文状態
- history：状態遷移の配列
- createdAt / updatedAt：作成・更新日時

### 5.5 history 要素の要件
- status：遷移後の状態
- at（occurredAt）：イベント発生時刻
- by（sourceService）：状態を変更したサービス
- eventId：イベント一意ID（冪等性確保用）
- metadata：任意の追加情報（任意）

---

## 6. 振る舞い要件（Behavior）

### 6.1 追記方式
- 履歴は **append-only（追記のみ）**
- 既存の履歴は更新・削除しない

### 6.2 冪等性
- 同一 eventId のイベントが再送された場合：
  - 履歴は追加しない
  - 処理は成功として扱う

### 6.3 書き込みタイミングと整合性戦略
**本DEMO実装方針（Kafka経由非同期）**:  
```
[OrderService - トランザクション処理]
  1. RDB(注文状態)更新 [トランザクション]
  2. Kafkaイベント発行 (OrderStatusChanged)
  
[OrderService - Kafka Consumer] ※推奨パターン
  3. Kafkaイベント受信
  4. MongoDB(order_audit)追記 [非同期、冪等性保証]
```

**設計判断の理由**:
- **RDBとMongoDBの疎結合**: MongoDB遅延がRDBトランザクションに影響しない
- **既存パターンとの一貫性**: alert-serviceと同じイベント駆動アーキテクチャ
- **リトライが容易**: Kafka Consumer Group機能を活用
- **パフォーマンス向上**: RDBコミット後すぐにレスポンス返却可能

**整合性の考え方**:
- RDBが正、MongoDBは「説明責任のための記録」として位置づけ
- Kafka経由で最終的な整合性を保証(Eventual Consistency)
- Consumer失敗時はKafkaの再配信メカニズムで自動リトライ
- 冪等性チェック(eventId)により重複書き込みを防止

```
// 商用での考慮点（本DEMOでは未実装）:
// - MongoDB書き込み失敗時のリトライキュー
// - 定期的な整合性チェックバッチ（RDB vs MongoDB）
// - Dead Letter Queue による失敗イベントの追跡
// - 監査要件が厳しい場合は分散トランザクション（Saga/2PC）の検討
```

---

## 7. 責務分離（重要）

### 7.1 書き込み責務
- MongoDB（order_audit）への書き込みは  
  **Order サービス（注文を統括するコンポーネント）のみ**が行う
- 決済・在庫など他サービスは、状態変化を通知する役割に専念する
- **設計上は OrderAuditService を論理分離**するが、**本DEMO実装では OrderService 内に統合**する  
  （Java クラスは `OrderAuditConsumer` として分離し、責務を独立させる）

**イベントフロー例(Kafka経由非同期)**:  
```
[Payment Service]
  └→ Kafka: PaymentCompleted イベント発行
       └→ [Order Service] Payment Consumer
            1. RDB: order.status を PAID に更新 (トランザクション)
            2. Kafka: OrderStatusChanged イベント発行
            
       └→ [Order Service] OrderStatusChanged Consumer
            3. Kafkaイベント受信
            4. MongoDB: order_audit に履歴追記 (非同期)
               - eventIdで冪等性チェック
               - 失敗時はKafkaが自動リトライ
```

**実装パターンの選択肢**:

**パターンA: OrderService内でConsumer実装(本DEMO推奨)**  
- メリット: シンプル、デプロイ構成が増えない、既存サービスで完結
- デメリット: OrderServiceの責務が若干増える
- 適用ケース: DEMO、中小規模システム
- **実装方針**: 
  - `OrderAuditConsumer.java` として**新規クラスを作成**（既存クラスと分離）
  - 既存の注文処理ロジックには影響を与えない
  - `@KafkaListener`アノテーションで独立したConsumerとして実装

**パターンB: 独立したOrderAuditServiceを作成**  
- メリット: 完全な責務分離、スケール独立、障害の影響範囲が限定的
- デメリット: サービス数増加、運用コスト増
- 適用ケース: 大規模システム、監査ログの重要度が非常に高い場合

**設計意図**:  
- 監査ログの整合性を単一サービスで保証(分散書き込みを避ける)
- 他サービスはドメインイベントの発行に専念(疎結合)
- Order Serviceが「注文の唯一の真実の源泉(Single Source of Truth)」
- Kafkaを介することでRDBとMongoDBを疎結合に保つ

### 7.2 データストアの役割分担
| データ内容 | ストア | 役割 |
|---|---|---|
| 注文の現在状態 | RDB | 正（トランザクション） |
| 状態遷移の履歴 | MongoDB | 証跡・監査 |

---

## 8. API 要件（Interface）

### 8.1 履歴取得
- 特定注文IDのタイムラインを取得できる
- 例：`GET /orders/{orderId}/timeline`

### 8.2 履歴追記（内部用）
- 状態遷移イベントを渡して履歴を追記できる
- 実装方式（REST / 内部メソッド）は既存設計に合わせて選択する

---

## 9. 実装ガイドライン（本DEMOでの実装方針）

### 9.1 Kafka Consumer実装(推奨パターン)
**OrderService内にConsumerを実装（論理分離／物理統合）**:  

> **⚠️ 実装時の注意**: 
> - **新規クラスとして作成**: `OrderAuditConsumer.java`
> - **既存クラスを修正しない**: 既存の注文処理ロジックと完全に分離
> - **配置場所**: `order-service/src/main/java/com/demo/ec/order/consumer/`
> - **責務**: Kafkaイベント受信 → MongoDB書き込みのみ

```java
@Service
@Slf4j
public class OrderAuditConsumer {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @KafkaListener(
        topics = "${kafka.topics.order-status-changed}",
        groupId = "order-audit-consumer-group"
    )
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChanged: orderId={}, status={}, eventId={}",
                 event.getOrderId(), event.getStatus(), event.getEventId());
        
        try {
            // 履歴エントリ作成
            HistoryEntry historyEntry = HistoryEntry.builder()
                .status(event.getStatus())
                .at(event.getOccurredAt())
                .by("order-svc")
                .eventId(event.getEventId())
                .metadata(Map.of(
                    "sourceEvent", event.getClass().getSimpleName(),
                    "sourceService", event.getSourceService()
                ))
                .build();
            
            // MongoDB更新（初回イベント対応: upsert + createdAt設定）
            Update update = new Update()
                .push("history", historyEntry)
                .set("currentStatus", event.getStatus())
                .set("updatedAt", Instant.now())
                .setOnInsert("createdAt", Instant.now())  // 初回のみ設定
                .setOnInsert("orderId", event.getOrderId()); // 初回のみ設定
            
            UpdateOptions options = new UpdateOptions().upsert(true);  // upsert有効化
            
            mongoTemplate.updateFirst(
                query(where("orderId").is(event.getOrderId())),
                update,
                OrderAudit.class,
                options
            );
            
            log.info("Order audit updated successfully: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to update order_audit: orderId={}, eventId={}",
                     event.getOrderId(), event.getEventId(), e);
            // Kafkaが自動的に再配信する(リトライ戦略はKafka設定で制御)
            throw e; // 例外を再スローしてKafkaにリトライさせる
        }
    }
}
```

**Kafka Consumer設定例**:  
```yaml
spring:
  kafka:
    consumer:
      group-id: order-audit-consumer-group
      enable-auto-commit: false  # 手動コミット
      auto-offset-reset: earliest
    listener:
      ack-mode: record  # レコード単位でコミット
```

### 9.2 冪等性チェックの実装
**推奨方式A（本DEMO採用）**: 専用フィールドでeventId管理  
```java
// データモデルに processedEventIds 配列を追加
// { "orderId": "ORD-1001", "processedEventIds": ["evt-123", "evt-456"], "history": [...] }

Update update = new Update()
    .addToSet("processedEventIds", event.getEventId())  // eventIdのみを配列に追加（重複自動スキップ）
    .push("history", historyEntry)
    .set("currentStatus", event.getStatus())
    .set("updatedAt", Instant.now());

UpdateResult result = mongoTemplate.updateFirst(
    query(where("orderId").is(event.getOrderId())
         .and("processedEventIds").ne(event.getEventId())),  // eventIdが未処理の場合のみ
    update,
    OrderAudit.class,
    new UpdateOptions().upsert(true)
);

if (result.getModifiedCount() == 0) {
    log.info("Duplicate eventId detected, skipping: {}", event.getEventId());
}
```

**推奨方式B**: $elemMatch による厳密チェック  
```java
// history配列内のeventIdを直接チェック（方式Aより複雑だが専用フィールド不要）
Query query = query(where("orderId").is(event.getOrderId())
    .and("history").not().elemMatch(where("eventId").is(event.getEventId())));

Update update = new Update()
    .push("history", historyEntry)
    .set("currentStatus", event.getStatus())
    .set("updatedAt", Instant.now());

UpdateResult result = mongoTemplate.updateFirst(query, update, OrderAudit.class, new UpdateOptions().upsert(true));
```

**⚠️ 非推奨パターン**: $addToSet でサブドキュメント全体を追加  
```java
// ❌ 誤り: $addToSet はドキュメント全体の一致で判定するため、
// eventId が同じでも timestamp などが異なれば重複して追加される
Update update = new Update()
    .addToSet("history", historyEntry)  // これでは冪等性を保証できない
    .set("currentStatus", event.getStatus());
```

**レガシー方式**: アプリケーション層での軽量チェック（非推奨: race condition リスク）  
```javascript
// MongoDB クエリ例
db.order_audit.findOne({
  orderId: "ORD-1001",
  "history.eventId": "evt-123"
})
// → 存在すれば処理スキップ、存在しなければ $push で履歴追加
// ⚠️ read-then-write なので並行処理で重複の可能性
```

**必要なインデックス**:  
```javascript
// 主キー的アクセス用（必須）
db.order_audit.createIndex({ "orderId": 1 }, { unique: true })

// 冪等性チェック用（推奨: 配列内eventIdの一意性保証）
db.order_audit.createIndex({ "history.eventId": 1 })

// 商用環境での強化案: 複合ユニークインデックス
// db.order_audit.createIndex(
//   { "orderId": 1, "history.eventId": 1 }, 
//   { unique: true, partialFilterExpression: { "history.eventId": { $exists: true } } }
// )
```

```
// 商用での改善案（本DEMOでは未実装）:
// - Redis等でeventId を短期間キャッシュ（高速チェック）
// - 別コレクションで processed_events を管理（TTLインデックスで自動削除）
// - Kafka Streams の状態ストアを活用
// 
// Kafka経由の場合:
// - Dead Letter Topic (DLT) で失敗イベントを隔離
// - Consumer Lag監視（遅延検知）
// - Partitioningによる並列処理最適化
```

### 9.3 ドキュメント肥大化対策
**本DEMO想定**:  
- 1注文あたり最大50件の状態遷移を想定  
- MongoDB 16MB制限は現実的に到達しない  

```
// 商用でのスケーラビリティ考慮（本DEMOでは未実装）:
// - 100件を超える場合は別ドキュメントに分割
// - Time-series Collection の活用（MongoDB 5.0+）
// - 古い履歴のアーカイブ（S3等への移動）
```

### 9.4 エラーハンドリングとリトライ戦略
**Kafka経由の場合のエラー処理**:  
```java
// Consumer内での例外処理
@KafkaListener(topics = "order-status-changed")
public void handleEvent(OrderStatusChangedEvent event) {
    try {
        updateOrderAudit(event);
    } catch (Exception e) {
        log.error("Failed to update order_audit, will be retried by Kafka: eventId={}",
                 event.getEventId(), e);
        // 例外を再スローすることでKafkaが自動リトライ
        throw e;
    }
}
```

**リトライ戦略**:  
```yaml
# application.yml
spring:
  kafka:
    listener:
      ack-mode: record
    consumer:
      # 最大リトライ回数と間隔はKafka側で制御
      max-poll-records: 10
      
# 商用では以下も検討:
# - Dead Letter Topic (DLT): リトライ上限後の失敗イベント格納
# - リトライ間隔の指数バックオフ
# - アラート通知(Slack/PagerDuty等)
```

---

## 10. 非機能・運用要件

### 10.1 インデックス戦略
**本DEMO実装**:  
```javascript
// 必須インデックス
db.order_audit.createIndex({ "orderId": 1 }, { unique: true })
db.order_audit.createIndex({ "history.eventId": 1 })

// 管理・調査用（オプション）
db.order_audit.createIndex({ "updatedAt": -1 })
```

### 10.2 保守性
- シンプルで理解しやすい実装を優先する
- 書き込み競合や複雑なロックを避ける
- ドメインイベント駆動で疎結合を保つ

### 10.3 監視・運用（商用考慮点）
```
// 本DEMOでは未実装だが、商用では以下が必要:

[モニタリング]
- MongoDB書き込み失敗率・レイテンシ
- RDB-MongoDB間の整合性チェック（差分件数）
- ドキュメントサイズの分布（肥大化検知）

[バックアップ・保持期間]
- 日次バックアップ（Point-in-Time Recovery）
- 保持期間: 法的要件に基づき設定（例: 7年）
- アーカイブ戦略: 1年以上経過した履歴はコールドストレージへ

[セキュリティ・監査要件]
- 履歴の改ざん防止（アプリケーション層で更新・削除を禁止）
- アクセスログの記録（誰がいつ履歴を参照したか）
- GDPR対応: 個人情報削除要求への対応方針
```

---

## 11. 受け入れ条件（Acceptance Criteria）
- [ ] 注文IDを指定して履歴が取得できる
- [ ] 状態遷移イベントを追記できる（RDB更新と連動）
- [ ] 同一 eventId の再送でも履歴が重複しない
- [ ] MongoDB は履歴用途に限定されている（正データはRDB）
- [ ] Order Service以外はorder_auditに直接書き込まない

---

## 12. 一文まとめ
ec-demo では、注文の正データは RDB で管理し、  
状態遷移の履歴は MongoDB に append-only で保存する。  
**Order Serviceが唯一の書き込み責務を持ち、他サービスはイベント発行のみを行う。**

---

## 13. 参考: アーキテクチャパターン
本設計は以下のパターンを適用:
- **Event Sourcing（簡易版）**: 状態遷移の履歴を保存
- **CQRS**: 書き込み（RDB）と参照（MongoDB履歴）の分離
- **Saga Pattern**: 分散トランザクション管理（Seata使用）
- **Domain Event**: サービス間の疎結合な連携

```
// 発展的な検討（本DEMOの範囲外）:
// - フル Event Sourcing: イベントストアを唯一の真実の源泉に
// - Outbox Pattern: トランザクショナルメッセージング
// - Change Data Capture (CDC): RDBの変更をストリーム化
```
