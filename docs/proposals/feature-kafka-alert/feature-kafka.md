## デモの見せ場（3ステップ）

1. **購入リクエスト**: `./test-saga.sh` で注文作成（`PENDING → WAITING_PAYMENT`）
2. **決済イベント反映**: Webhook（優先）またはポーリング（フォールバック）で `PAID/FAILED` へ収束
3. **観測**: 
   - WebSocketで画面に即時反映
   - （kafka-alert稼働時）Kafka Streamsで不整合検知 → `alerts.order_payment_inconsistency.v1` に `AlertRaised` 出力
   - MySQL (ec_system.sys_pay_alert) でアラート履歴を確認

## Demo highlights (3 steps)

1. **Purchase request**: Create order with `./test-saga.sh` (`PENDING → WAITING_PAYMENT`)
2. **Payment event reflection**: Webhook (priority) or polling (fallback) converges to `PAID/FAILED`
3. **Observation**: 
   - Immediate screen updates via WebSocket
   - (When kafka-alert is running) Kafka Streams detects inconsistencies → outputs `AlertRaised` to `alerts.order_payment_inconsistency.v1`
   - Check alert history in MySQL (ec_system.sys_pay_alert)

## 演示亮点（3步骤）

1. **购买请求**: `./test-saga.sh` 创建订单（`PENDING → WAITING_PAYMENT`）
2. **支付事件反映**: Webhook（优先）或轮询（兜底）收敛到 `PAID/FAILED`
3. **观测**: 
   - WebSocket 即时反映到画面
   - （kafka-alert 运行时）Kafka Streams 检测不整合 → 向 `alerts.order_payment_inconsistency.v1` 输出 `AlertRaised`
   - MySQL (ec_system.sys_pay_alert) 确认告警历史
