
## 背景目的
ec-demo用下記機能を実現したい、転職アピール用、テックリーダとして


## 機能実現概要
**kafka-alert**: Kafka Streamsで Rule A/B/C の不整合を検知し、運用アラート（`AlertRaised`）に変換
  - **Rule A**: 決済成功なのに注文が未反映（例: PayPay決済完了 → 注文`WAITING_PAYMENT`のまま）
  - **Rule B**: 注文`PAID`なのに決済が失敗（例: 注文支払済み → PayPay側は`FAILED`）
  - **Rule C**: 二重決済
- Rule C: 同一 `orderId` で `PaymentSucceeded` が複数回 → 二重決済疑い `AlertRaised`（重大度 `P1`）

サンプルはすでに実現済み
- sample設計
  _docs/feature-kafka-alert/sample/kafka-alert.md
  _docs/feature-kafka-alert/sample/kafka-alert-process.md

- サンプルのCode
alert-process-service
alert-streams-service  
test-rule-abc.sh

ec-demoに実現したです。


- 制限
サービスは最小限に追加する
alert-service追加
既存のCodeをなるべく最小限修正


## 設計提案
参照（ルール定義）: `_docs/feature-kafka-alert/kafka-alert-rule.drawio`  
成果物（ec-demo シーケンス図）: `_docs/feature-kafka-alert/ec-demo-kafaka-alert.drawio`

### 方針（最小差分）
- **既存サービスの責務は増やしすぎない**: `order-service` はドメイン処理のまま、kafka-alert は「検知」、alert-process は「蓄積/通知」に寄せる
- **イベント契約は薄く**: まずは JSON で PoC（`eventType`/`occurredAt`/`orderId`）から開始し、必要になったら契約強化（Schema）する
- **冪等性は必須**: Kafka再配信/再処理前提で、Streams側とDB側の両方で重複耐性を持つ

### コンポーネント構成
- **既存（ec-demo）**
  - `front` / `bff`: 画面/API（決済開始・状態参照）
  - `order-service`: 注文状態管理、決済連携（Webhook/ポーリング等）
- **追加（または既存ディレクトリを正式採用）**
  - `alert-streams-service`（Kafka Streams）: Rule A/B/C を検知し `AlertRaised` を publish
  - `alert-process-service`（Consumer）: `AlertRaised` を consume → DBへUPSERT（必要なら通知）
- **永続化**
  - MySQL: `ec_system.sys_pay_alert`（運用・管理画面向けの最小テーブル）

### Kafka トピック（設計）
- 入力（key は `orderId` 統一）
  - `orders.events.v1`（例: `OrderConfirmed`）
  - `payments.events.v1`（例: `PaymentSucceeded`）
- 出力
  - `alerts.order_payment_inconsistency.v1`（`AlertRaised`）

### イベント契約（最小）
- `OrderConfirmed`（例）
  - `eventType`, `eventId`, `occurredAt`, `orderId`
- `PaymentSucceeded`（例）
  - `eventType`, `eventId`, `occurredAt`, `orderId`, `paymentId`, `provider`, `amount`, `currency`
- `AlertRaised`（出力・例）
  - `alertId`, `rule`（A/B/C）, `severity`（P1/P2）, `orderId`, `detectedAt`, `facts`（根拠を JSON で保持）

### ルール検知ロジック（alert-streams-service）
- **StateStore（orderId単位）**: `orderConfirmedAt`, `paymentSucceededAt`, `paymentSuccessCount`, `ruleADeadline`, `ruleBDeadline`, `fired(A/B/C)`
- **Rule A（P2）**: `PaymentSucceeded` 観測後、`T_confirm` 以内に `OrderConfirmed` が来ない → `AlertRaised(A,P2)`
- **Rule B（P2）**: `OrderConfirmed` 観測後、`T_pay` 以内に `PaymentSucceeded` が来ない → `AlertRaised(B,P2)`
- **Rule C（P1）**: 同一 `orderId` で `PaymentSucceeded` が複数回 → 即 `AlertRaised(C,P1)`
- **実装要点**
  - `Transformer + StateStore + Punctuator` で deadline 超過を定期チェック
  - 再処理/再配信を考慮して `fired` を持ち、同一ルールの二重発火を抑止

### alert-process-service（運用寄りの責務）
- `alerts.order_payment_inconsistency.v1` を consume し、以下を実施
  - ログ（`orderId`, `alertId`, `rule`, `severity`）
  - DBへ UPSERT（重複受信に強くする）
  - （任意）Slack/Teams など通知連携（PoCは後回しでも可）

### 既存コードへの影響を最小化するポイント
- `order-service` は **「状態が確定した瞬間」** にイベントを publish（同期連携にしない）
  - 例: 注文が「支払確定」になったタイミングで `OrderConfirmed`
  - 例: PayPay 成功を「観測」したタイミングで `PaymentSucceeded`
- key は必ず `orderId` に寄せる（Streams join / state を単純化）
- 期限（`T_confirm`, `T_pay`）は `application.yml` で調整可能にする（デモ用に短縮できる）
- サンプルCodeをなるべく再利用するように。
