# ECサイト 決済システム デモ

PayPay決済を統合したECサイトのマイクロサービス実装デモです。外部決済の非同期性を前提に、**Seata Saga**で最終整合性を担保し、**Webhook + ポーリング**で堅牢に状態遷移させます。

> English: `README.en.md` / 中文: `README.zh-CN.md`

## リーダー視点の設計ポイント（要約）

- **BFF + WebSocket**: UI最適化とリアルタイム通知をBFFに集約し、変更耐性とUXを両立
- **Seata Saga**: サービス間の一貫性を「状態機械 + 補償」に落とし込み、運用可能な形で最終整合性を実装
- **Hybrid監視**: Webhook優先、ポーリングをフォールバックにして外部決済の欠落/遅延に耐える
- **冪等性**: `payment_last_event_id` によりイベント重複・順不同を安全に吸収
- **kafka-alert**: Kafka Streamsで Rule A/B/C の不整合を検知し、運用アラート（`AlertRaised`）に変換
  - **Rule A**: 決済成功なのに注文が未反映（例: PayPay決済完了 → 注文`WAITING_PAYMENT`のまま）
  - **Rule B**: 注文`PAID`なのに決済が失敗（例: 注文支払済み → PayPay側は`FAILED`）
  - **Rule C**: 期限切れ/失敗後に決済成功（例: 注文`FAILED`後 → PayPayから遅延成功通知）

## 成果指標（デモで示す価値）

- **決済完了→注文反映**: `PAID` 反映まで *60秒以内（99%）* を目標（Webhook優先 / ポーリングで収束）
- **外部イベント欠落への耐性**: Webhook未達でもポーリングで `PAID/FAILED` へ収束（待機中注文を監視）
- **整合性の可視化**: 不整合は Rule A/B/C として検知し、`alerts.order_payment_inconsistency.v1` に `AlertRaised` を出力

## 非機能の設計判断（要約）

- **Observability**: `actuator/health` を全サービスに標準化し、障害切り分けを高速化
- **Resilience**: Webhook優先 + ポーリングフォールバック、冪等性、補償トランザクションで安全に収束
- **Security**: Firebase ID Token検証 + Redisセッション、Cookieは `HttpOnly` / `SameSite` / `Secure` を前提
- **Fail-safe**: Redis障害など重要依存が壊れた場合は 503 を返し安全側に倒す（詳細は `_docs/`）

## リスクと対策（抜粋）

| リスク | 影響 | 対策 |
|---|---|---|
| PayPayのWebhook欠落/遅延 | 注文が `WAITING_PAYMENT` のまま | ポーリングで収束（Webhook優先） |
| Webhook再送/重複イベント | 二重更新・整合性崩れ | `payment_last_event_id` で冪等化 |
| 決済成功/失敗の順不同到達 | `PAID` 後に失敗が来る等 | `PAID` 後の失敗は状態変更せず記録のみ |
| Saga途中失敗（在庫/注文/決済の不整合） | 最終整合性が崩れる | 補償トランザクションで巻き戻し、kafka-alertで検知 |
| Redisセッション障害 | 認証/認可の劣化 | `/api/**` は 503（安全側）に倒して復旧を促す |

## デモの見せ場（3ステップ）

1. **購入リクエスト**: `./test-saga.sh` で注文作成（`PENDING → WAITING_PAYMENT`）
2. **決済イベント反映**: Webhook（優先）またはポーリング（フォールバック）で `PAID/FAILED` へ収束
3. **観測**: WebSocketで画面に即時反映、（kafka-alert稼働時）Rule A/B/C の `AlertRaised` をトピックで確認

## サービス概要

- **BFF (Backend for Frontend)**（port 8080）
  - WebSocketによるリアルタイム通知
  - PayPay決済API統合（Webhook受信/署名検証/注文サービスへの伝搬）
  - Redisセッション（Firebase ID Token検証 → `sid`発行）
- **order-service**（port 8081）
  - 注文作成・状態管理（`PENDING → WAITING_PAYMENT → PAID/FAILED`）
  - Sagaオーケストレーション（在庫確定/補償）
  - 決済ステータス更新（Webhook/ポーリングの共通ロジック）
- **storage-service**（port 8082）
  - 在庫の確保・確定・補償処理
- **account-service**（port 8083）
  - アカウント/残高管理

## 技術スタック

### フロントエンド
- **Vue 3** - モダンなSPAフレームワーク
- **TypeScript** - 型安全性の確保
- **Vite** - 高速な開発環境

### バックエンド
- **JDK 17** - LTS前提の統一ランタイムで運用コストを最小化
- **Spring Boot 3.x** - エンタープライズJavaフレームワーク
- **モダンマイクロサービス** - ドメイン境界で独立開発・独立デプロイ
- **分散トランザクション管理 (Saga)** - Seata Sagaモードによる最終整合性制御
- **WebSocket** - リアルタイムな決済ステータス通知
- **Spring Cloud OpenFeign** - サービス間通信

### インフラ・ミドルウェア
- **Docker / Docker Compose** - コンテナ化とオーケストレーション
- **MySQL 8.0** - リレーショナルデータベース
- **Seata 2.0** - 分散トランザクション管理フレームワーク
- **Redis** - ログインセッションの高速キャッシュ
- **Firebase Authentication** - ID Token検証による認証基盤
- **Kafka / Kafka Streams** - Rule A/B/Cで不整合検知し`alerts.order_payment_inconsistency.v1`へ`AlertRaised`出力
- **Elasticsearch** - 商品検索とオートコンプリート
- **MyBatis-Plus** - ORMフレームワーク

### 監視・運用
- **Spring Boot Actuator** - ヘルスチェック・メトリクス収集

## 図とドキュメント（詳細は `_docs/` 参照）

- 図（Draw.io）: `docs/ec-demo-architecture.drawio`（ページ: Overview / DataFlow）
- 起動手順（確定版）: `_docs/runbook/README_LOCAL_SETUP.md`
- アーキテクチャ詳細（Saga保証範囲、状態機械、kafka-alert契約、非機能）: `_docs/architecture/README_ARCHITECTURE.md`
- デプロイ手順（VPS想定）: `_docs/docker/demo/deploy.md`
