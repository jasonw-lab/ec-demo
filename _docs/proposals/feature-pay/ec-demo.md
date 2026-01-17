# ec-demo 機能まとめ（決済 / マイクロサービス / MongoDB / Seata / Kafka）

## 1. 目的と設計思想
- EC 注文〜決済は long business（最終的整合性を前提）
- トランザクションは短く、外部決済の完了は Saga に含めない
- Order Service を「唯一の真実」として状態遷移を統括

## 2. サービス構成と責務
- BFF: フロント向け API、注文作成、注文状態の取得、Kafka 受信と WS 通知
- Order Service: 注文ライフサイクルの統括、Saga オーケストレーション、状態遷移、監査ログ
- Inventory Service: 在庫の予約/確定/解放
- Payment Service: PayPay API + Webhook 受信、非同期決済の吸収
- Seata Server (Saga): Saga 実行基盤
- Kafka: 注文状態イベントの配信
- MongoDB: order_audit（注文監査ログ）

## 3. 状態モデル
- Order: CREATED → PAYMENT_PENDING → PAID / CANCELLED
- Inventory: AVAILABLE → RESERVED → COMMITTED / RELEASED
- Payment: INIT → PENDING → SUCCESS / FAILED

## 4. Seata Saga の適用範囲
- Saga は「注文初期化フェーズ」のみ（長期整合性の一部）
- Saga 名: order_initialization_saga
- Saga に含める処理
  - InitOrder（注文作成 → PAYMENT_PENDING）
  - reserveStock（在庫予約）
  - requestPayment（PayPay 決済要求）
- Saga に含めない処理
  - 決済完了待ち（Webhook）
  - 返金

## 5. 決済フロー概要
### 5.1 正常系（決済成功）
1) BFF → Order Service: createOrderSaga
2) Saga: InitOrder → reserveStock → requestPayment
3) Payment Service → PayPay: createPayment
4) Webhook SUCCESS: Payment Service → Order Service
5) Order Service: Order=PAID, Inventory=COMMITTED

### 5.2 異常系（決済失敗 / タイムアウト）
- Webhook FAILED または timeout
- Order Service: Order=CANCELLED
- Inventory: RELEASED
- 監査ログには CANCELLED (PAYMENT_FAILED / TIMEOUT / INVENTORY_SHORTAGE など) を記録

## 6. Kafka（イベント連携）
- Order Service が OrderStatusChanged を発行
- Topic: ec-demo.orders.events.v1
- BFF が consume して WebSocket に橋渡し
- 目的: フロントへの即時通知（状態変化の反映）

## 7. MongoDB（order_audit）
- 注文の現在状態は RDB、履歴は MongoDB（append-only）
- 書き込みは Order Service のみ
- 状態遷移の前後で必ず履歴を追記し、監査性を担保
- eventId による冪等性（Webhook 重複に対応）

## 8. 冪等性と監査性
- Webhook は eventId で重複排除
- 監査ログに理由を明記
  - CANCELLED (PAYMENT_FAILED)
  - CANCELLED (TIMEOUT)
  - CANCELLED (INVENTORY_SHORTAGE)

## 9. 主要な設計ポイントまとめ
- Order Service が唯一の真実（状態ガードを必須とする）
- Seata Saga は「初期整合性」のみを担う
- 外部決済完了は Saga 外で処理（長期整合性の分離）
- Kafka は通知基盤、MongoDB は監査基盤

