# ECサイト 決済システム デモ

PayPay決済を統合したECサイトのマイクロサービスアーキテクチャ実装デモです。分散トランザクション管理により、注文・在庫・決済処理を一貫性を保ちながら処理します。

## サービス概要

本システムは以下のマイクロサービスで構成されています：

- **BFF (Backend for Frontend)** - フロントエンドとバックエンドサービス間のアダプター層
  - WebSocketによるリアルタイム通知
  - PayPay決済API統合
  - QRコード生成
- **order-service** - 注文管理サービス（port 8081）
  - 注文作成・状態管理
  - 分散トランザクション制御
  - 決済ステータス管理
- **storage-service** - 在庫管理サービス（port 8082）
  - 在庫の確保・確定・補償処理
- **account-service** - アカウント管理サービス（port 8083）
  - ユーザー残高管理

## 技術スタック

### フロントエンド
- **Vue 3** - モダンなSPAフレームワーク
- **TypeScript** - 型安全性の確保
- **Vite** - 高速な開発環境

### バックエンド
- **Spring Boot 3.x** - エンタープライズJavaフレームワーク
- **マイクロサービスアーキテクチャ** - サービス間の疎結合設計
- **分散トランザクション管理 (Saga)** - Seata Sagaパターンによる分散トランザクション制御
- **WebSocket** - リアルタイムな決済ステータス通知
- **Spring Cloud OpenFeign** - サービス間通信

### インフラ・ミドルウェア
- **Docker / Docker Compose** - コンテナ化とオーケストレーション
- **MySQL 8.0** - リレーショナルデータベース
- **Seata 2.0** - 分散トランザクション管理フレームワーク
- **MyBatis-Plus** - ORMフレームワーク

### 監視・運用
- **Spring Boot Actuator** - ヘルスチェック・メトリクス収集

## 業務内容

- **ECサイト** - 商品閲覧、カート、注文フロー
- **PayPay決済統合** - QRコード決済、Webhook通知、決済ステータス管理

---

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

---

## 実装の工夫とベストプラクティス

### 1. 冪等性の確保

**課題**: Webhookの再送やネットワークエラーによる重複処理

**解決策**:
- `payment_last_event_id`による重複イベント検知
- ステータス遷移の前後チェック（例: `PAID`状態の注文に対して再度`PAID`イベントが来ても無視）
- トランザクション境界での状態チェック

**実装箇所**: `OrderPaymentService.handlePaymentStatus()`

### 2. エラーハンドリングと補償処理

**タイムアウト処理**:
- 決済有効期限（`payment_expires_at`）を設定
- スケジューラーによる定期的なタイムアウト検知
- 自動的な在庫補償処理

**失敗時の補償**:
- Sagaパターンによる自動補償フロー
- 在庫予約の自動解放
- 注文ステータスの適切な更新

### 3. 関心の分離とカプセル化

**Webhook処理とポーリング処理の分離**:
- `PaymentWebhookHandler`: Webhook専用の処理ロジック
- `PaymentStatusPollingService`: ポーリング専用の処理ロジック
- `OrderPaymentService`: 共通のビジネスロジック（両方から呼び出される）

**メリット**: テスト容易性、保守性、拡張性の向上

### 4. 型安全性と開発体験

**フロントエンド**:
- TypeScriptによる型安全性
- Vue 3 Composition APIによるリアクティブな状態管理

**バックエンド**:
- JacksonによるJSONマッピング（スネークケース ↔ キャメルケース）
- 詳細なログ出力によるデバッグ容易性
- 適切な例外処理とエラーメッセージ

### 5. 監視と運用性

**Spring Boot Actuator**:
- ヘルスチェックエンドポイント
- メトリクス収集

**ログ設計**:
- 構造化ログ（`[PaymentService]`, `[PaymentPolling]`などのプレフィックス）
- トレーサビリティのための相関ID
- デバッグレベルの詳細ログ

---

## 技術的な課題と解決策

### 課題1: 外部決済サービスの非同期性

**問題**: PayPay決済は非同期で、即座に結果が返らない

**解決策**:
1. 注文を`WAITING_PAYMENT`状態で保持
2. Webhookまたはポーリングで結果を取得
3. 結果に応じて状態遷移（`PAID` / `FAILED`）

### 課題2: Webhookの信頼性

**問題**: Webhookが届かない、または遅延する可能性

**解決策**:
1. Webhookを優先的に処理（リアルタイム性）
2. ポーリングをフォールバックとして実装（確実性）
3. 両方の仕組みで重複通知を防止（冪等性）

### 課題3: 分散トランザクションの一貫性

**問題**: 複数のマイクロサービス間でデータ整合性を保つ

**解決策**:
- Sagaパターンによる最終整合性の保証
- 補償トランザクションによるロールバック
- 状態機械による明確なフロー定義

### 課題4: フロントエンドへのリアルタイム通知

**問題**: 決済完了をユーザーに即座に通知する必要がある

**解決策**:
- WebSocketによる双方向通信
- 接続時のスナップショット送信
- ステータス変更時の即座なプッシュ通知

---

## パフォーマンスとスケーラビリティへの配慮

### 1. 非同期処理による応答性の向上

- **決済処理の非同期化**: 注文作成時に決済結果を待たず、`WAITING_PAYMENT`状態で即座にレスポンス
- **WebSocketによる効率的な通知**: ポーリングよりも低レイテンシでステータス更新を通知
- **バッチ処理**: タイムアウト検知をスケジューラーでバッチ処理し、DB負荷を分散

### 2. リソース管理

- **接続管理**: WebSocketセッションの適切な管理とクリーンアップ
- **メモリ効率**: 完了した注文の監視状態を適切に解放
- **DB接続プール**: HikariCPによる効率的なDB接続管理

### 3. スケーラビリティ

- **ステートレス設計**: 各サービスはステートレスに設計され、水平スケールが可能
- **分散トランザクション**: Seataによる分散トランザクション管理により、サービスを独立にスケール可能
- **マイクロサービス分離**: 注文、在庫、アカウントを独立したサービスとして分離し、個別にスケール可能

### 4. 可用性の向上

- **フォールバック機構**: Webhook失敗時のポーリングによる確実な処理
- **重複処理の防止**: 冪等性により、リトライや再送に対しても安全
- **タイムアウト処理**: 決済が完了しない場合でも、自動的にタイムアウト処理を実行

---

## 開発・運用面での工夫

### 1. コード品質

- **関心の分離**: Webhook処理、ポーリング処理、ビジネスロジックを明確に分離
- **単一責任の原則**: 各クラス・メソッドが明確な責任を持つ
- **DRY原則**: 共通ロジックを`OrderPaymentService`に集約

### 2. テスト容易性

- **依存性注入**: SpringのDIにより、モック化が容易
- **インターフェース分離**: クライアントインターフェースにより、実装の差し替えが可能
- **明確な境界**: サービス間の境界が明確で、統合テストが容易

### 3. 運用性

- **構造化ログ**: プレフィックスによるログ分類と検索容易性
- **ヘルスチェック**: Actuatorによるサービス状態の監視
- **設定の外部化**: プロファイルによる環境別設定

### 4. 保守性

- **ドキュメント**: Javadocによる詳細な説明
- **命名規則**: 明確で一貫性のある命名規則
- **コードコメント**: 複雑なロジックに対する適切なコメント

---

## 技術スタック（詳細）
- Java 17
- Spring Boot 3.x
- Seata 2.0
- MyBatis-Plus
- MySQL 8.0
- Docker Compose


## 事前準備（MySQL と Seata Server）
- MySQL はリポジトリ直下の `_docker/docker-compose-mysql.yml` で起動します（8.0, 3307, Apple Silicon/M1 対応）。
  ```bash
  cd _docker
  docker compose -f docker-compose-mysql.yml up -d
  ```
- 起動時に自動作成されるもの:
  - DB: seata_order, seata_storage, seata_account, seata
  - テーブル: 各ビジネス DDL＋undo_log、Seata メタテーブル
  - 初期データ: 在庫とアカウント残高
- 接続情報:
  - host: 127.0.0.1
  - port: 3307
  - user: root / pass: 123456
- Seata Server は `_docker/docker-compose-seata.yml` で起動します（2.0.0, Apple Silicon/M1 対応）。
  ```bash
  cd _docker
  docker compose -f docker-compose-seata.yml up -d
  ```
  - コンソール: http://127.0.0.1:7091
  - サーバーポート: 8091（application.yml の既定: server.port 7091 → service-port 8091）
  - ログ: `${basepath}/seata/logs` にホスト共有（`.env` の basepath を参照）
  - コンフィグ: `_docker/seata-2.0.0/conf/application.yml`（添付ファイルをベースに file/db モードで構成）

## ビルド/基本テスト
各サービスは Spring Boot アプリとしてビルドできます。現時点の自動テストは Actuator のヘルスのみです。
```bash
# 本ディレクトリで
mvn -q -DskipTests=false -pl order-service test
mvn -q -DskipTests=false -pl storage-service test
mvn -q -DskipTests=false -pl account-service test
```

## 実行（共通）
- Spring Profile は `saga` を使用します
- 例（それぞれ別ターミナルで起動）:
  ```bash
  mvn -q -pl storage-service spring-boot:run -Dspring-boot.run.profiles=saga
  mvn -q -pl account-service  spring-boot:run -Dspring-boot.run.profiles=saga
  mvn -q -pl order-service    spring-boot:run -Dspring-boot.run.profiles=saga
  mvn -q -pl bff              spring-boot:run
  ```

ヘルス確認:
```bash
curl -s localhost:8081/actuator/health | jq
curl -s localhost:8082/actuator/health | jq
curl -s localhost:8083/actuator/health | jq
```

---

## SAGA モード
- Profile: `saga`
- エンドポイント:
  - POST `http://localhost:8081/api/orders/saga`（State Language による SAGA 実行）
  - POST `http://localhost:8081/api/orders/saga/sample`（簡易サンプル）
  - POST `http://localhost:8081/api/orders/{orderNo}/payment/events`（BFF からの決済結果通知: status=COMPLETED / FAILED / TIMED_OUT など）
- ステートマシン定義:
  - `order-service/src/main/resources/statelang/order_create_saga.json`
- 簡易テスト:
  ```bash
  # 既定値(count=10, amount=10.0)
  ./test-saga.sh

  # パラメータを変えて異常系を再現
  ./test-saga.sh 999999 10.0     # 在庫不足
  ./test-saga.sh 1 1000000.0     # 残高不足
  ```
- メモ:
  - 各サービスの `application-saga.yaml` にて `tx-service-group: saga_tx_group` を利用します。
  - 決済開始後は Order が `WAITING_PAYMENT` 状態となり、BFF からの `COMPLETED / FAILED / TIMED_OUT` 通知で `PAID` / `FAILED` に確定します。
  - 待機中に `payment_expires_at` を過ぎた注文は `order.payment.timeout-check-interval-ms`（既定 60s）間隔でタイムアウト検知され、自動補償されます。
  - `t_order` には WebSocket 認証に利用する `payment_channel_token` / `payment_channel_expires_at` および `payment_last_event_id` を保持します。

### BFF（Webhook / WebSocket）
- `POST /api/orders/purchase` は `orderId` と併せて WebSocket 接続用 `channelToken` を返却します。
- PayPay Webhook は `POST /api/paypay/webhook` に送付します。payload 内の `merchantPaymentId`（=orderId）と `status` から Order Service へ転送され、重複イベントは `payment_last_event_id` で抑止します。
- WebSocket エンドポイント: `ws://localhost:8080/ws/orders?orderId={orderId}&token={channelToken}`
  - 接続直後に最新スナップショット（`type: "ORDER_STATUS"`）を 1 回送信します。
  - 決済確定/補償/タイムアウト時は `result: SUCCESS | FAILED | TIMEOUT` を push します。
- `GET /api/payments/{orderId}/details` などの REST API でも同じ情報を取得できます（channelToken も含む）。

---
