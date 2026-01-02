# EC 支付系统 Demo

这是一个集成 PayPay 的电商支付微服务 Demo。面向真实世界的“支付异步/事件丢失/延迟”场景：用 **Seata Saga** 实现最终一致性，并用 **Webhook + 轮询** 保障状态可靠收敛。

> 日本語: `README.md` / English: `README.en.md`

## 设计要点

- **BFF + WebSocket**：在 BFF 聚合 UI 友好的接口与实时通知，提高可演进性与用户体验
- **Seata Saga**：用“状态机 + 补偿”实现跨服务一致性（可运维的最终一致性）
- **混合监测**：Webhook 优先，轮询兜底，抵御外部事件缺失/延迟
- **幂等性**：通过 `payment_last_event_id` 吸收重复与乱序事件
- **kafka-alert**：Kafka Streams 检测 Rule A/B/C 不一致，并输出运维告警（`AlertRaised`）
  - **Rule A**：支付成功但订单未反映（例：PayPay支付完成 → 订单仍为`WAITING_PAYMENT`）
  - **Rule B**：订单`PAID`但支付失败（例：订单已支付 → PayPay显示`FAILED`）
  - **Rule C**：期限/失败后支付成功（例：订单`FAILED`后 → PayPay延迟成功通知）

## 成果指标（演示中展现的价值）

- **支付完成→订单反映**: `PAID` 反映目标 *60秒以内（99%）* （Webhook优先 / 轮询收敛）
- **外部事件缺失的耐性**: Webhook未达也可通过轮询收敛到 `PAID/FAILED` （监控待处理订单）
- **一致性的可视化**: 不一致作为 Rule A/B/C 检测，在 `alerts.order_payment_inconsistency.v1` 输出 `AlertRaised`

## 非功能设计判断（摘要）

- **Observability**: 全服务标准化 `actuator/health`，加速故障排查
- **Resilience**: Webhook优先 + 轮询兜底、幂等性、补偿事务安全收敛
- **Security**: Firebase ID Token验证 + Redis会话，Cookie前提 `HttpOnly` / `SameSite` / `Secure`
- **Fail-safe**: Redis故障等重要依赖损坏时返回 503，向安全侧倒（详情见 `_docs/`）

## 风险与对策（摘录）

| 风险 | 影响 | 对策 |
|---|---|---|
| PayPay的Webhook缺失/延迟 | 订单停留在 `WAITING_PAYMENT` | 轮询收敛（Webhook优先） |
| Webhook重发/重复事件 | 双重更新・一致性崩溃 | 通过 `payment_last_event_id` 实现幂等 |
| 支付成功/失败顺序颠倒到达 | `PAID` 后失败到来等 | `PAID` 后失败仅记录不改变状态 |
| Saga中途失败（库存/订单/支付不一致） | 最终一致性崩溃 | 补偿事务回滚，kafka-alert检测 |
| Redis会话故障 | 认证/授权劣化 | `/api/**` 返回 503（安全侧）促恢复 |

## 演示亮点（3步骤）

1. **购买请求**: `./test-saga.sh` 创建订单（`PENDING → WAITING_PAYMENT`）
2. **支付事件反映**: Webhook（优先）或轮询（兜底）收敛到 `PAID/FAILED`
3. **观测**: WebSocket即时反映到画面，（kafka-alert运行时）确认主题中的 Rule A/B/C `AlertRaised`

## 服务组成

- **BFF**（8080）
  - WebSocket 实时推送
  - PayPay 集成（Webhook 接收/校验 → 转发给 order-service）
  - Redis 会话（校验 Firebase ID Token → 下发 `sid`）
- **order-service**（8081）
  - 订单状态机（`PENDING → WAITING_PAYMENT → PAID/FAILED`）
  - Saga 编排（库存确认/补偿）
  - Webhook/轮询共用的支付状态更新逻辑
- **storage-service**（8082）
  - 库存预留/确认/补偿
- **account-service**（8083）
  - 账户/余额管理

## 技术栈（保留在 README）

### 前端
- **Vue 3**
- **TypeScript**
- **Vite**

### 后端
- **JDK 17**
- **Spring Boot 3.x**
- **微服务架构**
- **分布式事务（Saga）** - Seata Saga 模式
- **WebSocket**
- **Spring Cloud OpenFeign**

### 基础设施 / 中间件
- **Docker / Docker Compose**
- **MySQL 8.0**
- **Seata 2.0**
- **Redis**
- **Firebase Authentication**
- **Kafka / Kafka Streams**：检测 Rule A/B/C 并向 `alerts.order_payment_inconsistency.v1` 输出 `AlertRaised`
- **Elasticsearch**
- **MyBatis-Plus**

### 可观测性
- **Spring Boot Actuator**

## 架构图与文档（详细见 `_docs/`）

- Draw.io 架构图：`docs/ec-demo-architecture.drawio`（Overview / DataFlow）
- 本地启动手册：`_docs/runbook/README_LOCAL_SETUP.md`
- 架构深挖（Saga 范围、状态机、kafka-alert 契约、非功能）：`_docs/architecture/README_ARCHITECTURE.md`
- 部署手册（偏 VPS）：`_docs/docker/demo/deploy.md`
