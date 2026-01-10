
## 背景目的
ec-demo用下記機能を実現したい、転職アピール用、テックリーダとして


## 機能実現概要
**kafka-alert**: Kafka Streamsで Rule A/B/C の不整合を検知し、運用アラート（`AlertRaised`）に変換
  - **Rule A**: 決済成功なのに注文が未反映
  - **Rule B**: 注文`PAID`なのに決済が失敗
  - **Rule C**: 二重決済（同一 `orderId` で `PaymentSucceeded` が複数回 → 重大度 `P1`）


### 制限事項
- サービスは最小限に追加する（`alert-service` のみ追加）
- 既存のコードをなるべく最小限修正


## アーキテクチャ

### サービス構成
- **alert-service** (単一サービス内で統合)
  - **Kafka Streams 処理** (`OrderPaymentTransformer`)
    - Rule A/B/C の不整合を検知
    - StateStore で `orderId` 単位の状態管理
    - Punctuator で deadline 超過を定期チェック
  - **Consumer 処理** (`AlertRaisedConsumer` + `AlertProcessService`)
    - `AlertRaised` イベントを consume
    - MySQL (ec_system.sys_pay_alert) へ UPSERT
    - ログ出力

### Kafka トピック
- **入力** (key は `orderId` 統一)
  - `ec-demo.orders.events.v1`
    - イベント例: `OrderStatusChanged` (eventType フィールドで判定)
    - 注文確定を表すイベント（`newStatus == "PAID"` など）
  - `ec-demo.payments.events.v1`
    - イベント例: `PaymentSucceeded`
    - 決済成功を表すイベント
- **出力**
  - `ec-demo.alerts.order_payment_inconsistency.v1`
    - イベント: `AlertRaised`
    - 不整合検知時に送信

### イベント構造

#### 入力イベント (payments.events.v1)
```json
{
  "eventType": "PaymentSucceeded",
  "eventId": "<uuid>",
  "occurredAt": "<ISO8601>",
  "orderId": "O-1001",
  "paymentId": "P-9001",
  "provider": "PayPay",
  "amount": 1200,
  "currency": "JPY"
}
```

#### 入力イベント (orders.events.v1)
```json
{
  "eventType": "OrderStatusChanged",
  "eventId": "<uuid>",
  "occurredAt": "<ISO8601>",
  "aggregateId": "O-1001",
  "payload": {
    "orderId": "O-1001",
    "newStatus": "PAID"
  }
}
```

#### 出力イベント (alerts.order_payment_inconsistency.v1)
```json
{
  "alertId": "<uuid>",
  "rule": "A|B|C",
  "severity": "P1|P2",
  "orderId": "O-1001",
  "detectedAt": "<ISO8601>",
  "facts": {
    "orderConfirmedAt": "<ISO8601|null>",
    "paymentSucceededAt": "<ISO8601|null>",
    "paymentSuccessCount": 1
  }
}
```

## 検知ルール詳細

### StateStore 管理 (orderId 単位)
- **Store名**: `order-payment-store` (RocksDB)
- **State クラス**: `OrderPaymentState`
- **フィールド**:
  - `orderConfirmedAt`: String (ISO8601) - 注文確定日時
  - `paymentSucceededAt`: String (ISO8601) - 決済成功日時
  - `paymentSuccessCount`: int - 決済成功回数
  - `ruleADeadlineEpochMs`: Long - Rule A の期限 (epoch millis)
  - `ruleBDeadlineEpochMs`: Long - Rule B の期限 (epoch millis)
  - `ruleAFired`: boolean - Rule A 発火済みフラグ
  - `ruleBFired`: boolean - Rule B 発火済みフラグ
  - `ruleCFired`: boolean - Rule C 発火済みフラグ

### ルール検知ロジック (`OrderPaymentTransformer`)

#### Rule A (P2): 決済成功なのに注文が未反映
- **トリガー**: `PaymentSucceeded` 受信後、`T_confirm` 秒以内に `OrderConfirmed` (注文確定) が来ない
- **検知タイミング**: Punctuator (定期実行)
- **条件**:
  ```
  now >= ruleADeadlineEpochMs 
  AND orderConfirmedAt == null 
  AND !ruleAFired
  ```
- **アクション**: `AlertRaised(rule=A, severity=P2)` を emit

#### Rule B (P2): 注文確定なのに決済が失敗
- **トリガー**: `OrderConfirmed` 受信後、`T_pay` 秒以内に `PaymentSucceeded` が来ない
- **検知タイミング**: Punctuator (定期実行)
- **条件**:
  ```
  now >= ruleBDeadlineEpochMs 
  AND paymentSucceededAt == null 
  AND !ruleBFired
  ```
- **アクション**: `AlertRaised(rule=B, severity=P2)` を emit

#### Rule C (P1): 二重決済
- **トリガー**: 同一 `orderId` で `PaymentSucceeded` が複数回受信
- **検知タイミング**: リアルタイム (イベント受信時)
- **条件**:
  ```
  paymentSuccessCount >= 2 
  AND !ruleCFired
  ```
- **アクション**: 即座に `AlertRaised(rule=C, severity=P1)` を emit

### 実装要点
- `Transformer + StateStore + Punctuator` で deadline 超過を定期チェック
- 再処理/再配信を考慮して `fired` フラグで同一ルールの二重発火を抑止
- JSON parse 失敗時はログ出力してスキップ (ベストエフォート)

## データ永続化

### MySQL (ec_system.sys_pay_alert)
- **テーブル**: `sys_pay_alert`
- **主要カラム**:
  - `alert_id` VARCHAR(64) PRIMARY KEY
  - `order_id` VARCHAR(64) NOT NULL
  - `rule` VARCHAR(10) NOT NULL (A/B/C)
  - `severity` VARCHAR(10) (P1/P2)
  - `detected_at` TIMESTAMP(3)
  - `status` VARCHAR(20) (NEW/ACKNOWLEDGED/RESOLVED)
  - `message` TEXT
  - `facts_json` JSON - facts オブジェクトを JSON 型で保存
  - `created_at` TIMESTAMP(3)
  - `updated_at` TIMESTAMP(3)
- **UPSERT 戦略**: `ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP(3)`
  - 重複受信に強い (Kafka の at-least-once 配信を考慮)

### アラート処理フロー (`AlertProcessService`)
1. `AlertRaised` イベントを consume
2. 必須フィールド検証 (alertId, orderId, rule)
3. `facts` を JSON 文字列にシリアライズ
4. MySQL へ UPSERT
5. ログ出力 (`orderId`, `alertId`, `rule`, `severity`)

## 設定パラメータ

### タイムアウト設定 (application.yml)
```yaml
alert:
  t_confirm_seconds: 30      # Rule A: PaymentSucceeded → OrderConfirmed の猶予時間
  t_pay_seconds: 30          # Rule B: OrderConfirmed → PaymentSucceeded の猶予時間
  punctuate_interval_seconds: 10  # Punctuator の実行間隔
```

### Kafka 設定
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    streams:
      application-id: alert-service
      state-dir: ${java.io.tmpdir}/kafka-streams/alert-service
ec-demo:
  kafka:
    topics:
      orders-events: ec-demo.orders.events.v1
      payments-events: ec-demo.payments.events.v1
      alerts-order-payment-inconsistency: ec-demo.alerts.order_payment_inconsistency.v1
```

## 今後の拡張 (オプション)
- Slack/Teams など外部通知連携
- アラート管理 UI (status 更新機能)
- メトリクス収集 (Prometheus/Grafana)
