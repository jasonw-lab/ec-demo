# アーキテクチャ（詳細）

## 図（Draw.io）

- `docs/ec-demo-architecture.drawio`（ページ: Overview / DataFlow）

## 全体像と主要データフロー（要約）

- **全体像**: Front → BFF → 各サービス（order/storage/account）に加え、Seata・MySQL・Redis・Elasticsearch・Kafka Streams・PayPayが連携
- **主要フロー**: 注文作成 → 決済作成 → Webhook/ポーリングで結果取得 → 状態遷移 → WebSocket通知 / kafka-alert

## アーキテクチャの特徴と設計思想

### 1. BFF (Backend for Frontend) パターンの採用

**設計意図**: フロントエンドとバックエンドマイクロサービス間のアダプター層としてBFFを配置することで、以下のメリットを実現：

- **フロントエンド最適化**: フロントエンドが必要なデータ形式に変換し、不要な通信を削減
- **セキュリティ**: フロントエンドに直接バックエンドサービスを公開せず、BFF経由でアクセス制御
- **集約**: 複数のマイクロサービス呼び出しをBFFで集約し、フロントエンドの複雑性を低減

### 2. Sagaパターンによる分散トランザクション管理

**課題**: マイクロサービス間でACIDトランザクションを実現するのは困難

**解決策**: Seata Sagaパターンを採用し、最終整合性を保証：

- **状態機械ベース**: JSONステートマシン定義により、ビジネスフローを宣言的に記述
- **補償トランザクション**: 失敗時の自動ロールバック処理を定義
- **非同期処理**: 決済待ち状態を管理し、外部イベント（Webhook）で状態遷移

**実装例**: `order-service/src/main/resources/statelang/order_create_saga.json`

### 3. ハイブリッドな決済ステータス監視

**課題**: 外部決済サービス（PayPay）のWebhookが届かない場合の対応

**解決策**: Webhook優先、ポーリングをフォールバックとする二重監視：

- **Webhook優先**: リアルタイム性を重視し、Webhook受信時に即座に処理
- **ポーリングフォールバック**: BFFとorder-serviceの両方でポーリングを実装し、Webhook失敗時も確実に検知
- **重複防止**: `payment_last_event_id`によるイベント重複検知と冪等性保証

### 4. リアルタイム通知の実現

**WebSocketによる双方向通信**:
- 接続時に最新スナップショットを送信
- 決済ステータス変更時に即座にプッシュ通知
- チャネルトークンによる認証とセッション管理

**ポーリングとの併用**:
- WebSocket接続中の注文について、BFFが定期的にステータスをチェック
- 変更検知時にWebSocketで通知（Webhookが来ない場合の保険）

### 5. Kafka Streamsによる整合性アラート（kafka-alert）

**目的**: 分散環境で「注文」と「決済」のイベント到達順序や欠落に起因する整合性崩れを、運用観点で早期検知して復旧を加速します。

- **出力**: `alerts.order_payment_inconsistency.v1` に `AlertRaised`（`orderNo`キー / `ruleId` / `observed` / `expected` / `detectedAt` など）をpublish
- **Rule A（決済成功→注文未反映）**: 決済が `COMPLETED|SUCCESS|CAPTURED` を観測したにも関わらず、一定猶予後も注文が `WAITING_PAYMENT|PENDING` のまま
- **Rule B（注文PAID→決済不成立）**: 注文が `PAID` に遷移したのに、決済側は成功ステータスが観測できない／直近が `FAILED|CANCELLED|EXPIRED`
- **Rule C（期限/失敗後の遅延決済）**: `payment_expires_at` 超過または注文が `FAILED` になった後に決済成功を観測

## 分散トランザクションの保証範囲（Saga）

- **原子性の境界**: 各サービス内のDB更新は原子だが、外部決済（PayPay）やサービス間は原子にしない
- **最終整合性の到達条件**: Webhook/ポーリングで決済結果を観測 → `OrderPaymentService` で状態更新 → 在庫確定/補償の完了
- **補償が走る条件**: 失敗・タイムアウト・期限切れ時に在庫補償（`storageCompensate`）、成功時は在庫確定（`storageConfirm`）

## 決済状態機械（Webhook優先 / ポーリングはフォールバック）

| 状態 | 入口 | 遷移条件 | 次状態 | 備考 |
|---|---|---|---|---|
| PENDING | 注文作成 | 決済作成成功 | WAITING_PAYMENT | Saga開始 |
| WAITING_PAYMENT | 決済作成 | Webhook/ポーリングで成功 | PAID | `payment_last_event_id`で重複排除 |
| WAITING_PAYMENT | 決済作成 | 失敗/期限切れ/タイムアウト | FAILED | 補償トランザクション実行 |
| PAID | 決済成功 | 失敗イベントが遅延到達 | PAID | 整合性維持のため無視 |

## kafka-alert 入出力契約（設計想定）

- **入力イベント（想定）**
  - `order-status-changed`（orderNo, status, paymentStatus, updatedAt）
  - `payment-status-observed`（orderNo, status, eventId, occurredAt）
- **遅延許容ウィンドウ**: 監視猶予を持たせて誤検知を抑制（例: 5分）
- **重複排除**: `orderNo + ruleId + eventId`（または同一window内の同一キー）を抑止
- **出力トピック**: `alerts.order_payment_inconsistency.v1`

```json
{
  "alertId": "b2c3f4c0-2f3e-4b9c-9b5d-0a5c52f2d8b1",
  "ruleId": "RULE_A",
  "orderNo": "ORD-20240201-000123",
  "observed": {"paymentStatus": "COMPLETED", "orderStatus": "WAITING_PAYMENT"},
  "expected": {"orderStatus": "PAID"},
  "detectedAt": "2024-02-01T12:34:56Z",
  "source": "kafka-alert"
}
```

- **運用アクション**
  - Rule A: order-serviceのDB反映遅延/失敗を確認 → リカバリ（再送・補償）
  - Rule B: 決済の実在確認 → 不整合なら注文をロールバック
  - Rule C: 返金/補償判断（決済成功が遅延到達）

## 非機能（設計判断として明文化）

- **監視**: Actuatorの`health`/`info`、構造化ログ、相関IDでトレース可能にする設計
- **SLO例**: 決済完了 → 注文`PAID`反映まで *60秒以内（99%）* を目標
- **フェイルセーフ**: Webhook失敗時はポーリングで補完、Redis障害時は`/api/**`で503を返し安全側に倒す

