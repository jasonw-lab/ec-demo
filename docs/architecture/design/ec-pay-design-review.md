# ec-pay シーケンス図 設計レビュー指摘事項

## ✅ 修正済み

### 指摘-1: order-service と alert-service 間の eventType 不一致 [修正済み]

> **ブランチ**: `fix/alert-event-type-mismatch`

| サービス | 修正前 | 修正後 |
|---|---|---|
| alert-service `OrderPaymentTransformer.java` | `"OrderConfirmed"` をチェック | `"OrderStatusChanged"` + `payload.newStatus == "PAID"` をチェック |
| ec-alert.drawio Rule B | `produce OrderConfirmed` | `produce OrderStatusChanged(newStatus=PAID)` |

### 指摘-2: PaymentSucceeded publish 未記載 [修正済み]

`ec-pay.drawio` 成功パスに `publish PaymentSucceeded → ec-demo.payments.events.v1`（Payment Service → Kafka、オレンジ色矢印）を追加。

## ✅ 設計正常確認

| 確認項目 | 結果 |
|---|---|
| Saga フロー | ✅ |
| Webhook 決済成功/失敗フロー | ✅ |
| Kafka publish (OrderStatusChanged) | ✅ |
| OrderAudit (Kafka→MongoDB) | ✅ |
| Payment Service → Kafka (PaymentSucceeded) | ✅ |
