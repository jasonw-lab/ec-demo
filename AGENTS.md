# ec-demo — エージェント向けガイド

このファイルは ec-demo 固有のルールのみを記載する。汎用ルールは `~/ai-rules` を参照。

## 共通ルール（必読）

作業開始時に以下を読み、そのルールに従う。

- `~/ai-rules/ai-common.md` — 言語方針、実装・テスト・コミット規約、自律実行ポリシー、完了報告フォーマット
- `~/ai-rules/AGENTS.md` — Knowledge Base (`kb`) の定義
- `~/ai-rules/PROJECTS.md` — 全体プロジェクトマップ

共通ルールと本ファイルが衝突する場合は、ユーザーの明示指示 > 本ファイル > 共通ルール の順で優先する。

## プロジェクト概要

PayPay 決済をマイクロサービスへ統合した EC デモ。分散トランザクションは **Seata Saga**、
決済ステータス同期は **Webhook + Polling** のハイブリッド。ポートフォリオ用途も兼ねるため、
可読性・保守性・説明可能性を優先する。

## リポジトリ構成

```
apps/bff              # BFF (REST/WebSocket, Firebase 認証, PayPay webhook, Redis セッション)
apps/services/*       # order / storage / account / payment / alert / es
apps/web              # Vue 3 + TypeScript (Vite)
platform/docker/local # ローカル用ミドルウェア Compose
platform/docker/demo  # デモ/VPS 用 Compose（アプリ含む）
scripts/              # フロー・スモークテスト用スクリプト
docs/                 # adr / architecture / runbook / proposals / worklog
```

## 主要コマンド

```bash
mvn clean package -DskipTests           # バックエンド全モジュールのビルド
mvn test                                # 全モジュールのテスト
mvn test -pl apps/services/order-service # 単一モジュールのテスト
mvn spring-boot:run -pl apps/bff        # 単一サービス起動（モジュールパスを差し替え）

cd apps/web && pnpm install && pnpm dev # フロントエンド開発
cd apps/web && pnpm build               # フロントエンド本番ビルド

./scripts/test-saga.sh 1 1              # 注文/Saga フローのスモークチェック
./init.sh pull                          # 環境ファイルの同期
curl http://localhost:8080/actuator/health  # BFF ヘルスチェック
```

## アーキテクチャ制約

Hybrid Hexagonal レイヤリング（各サービス `com.demo.ec.[service]` 配下）:

```
web/ → application/ → domain/ ← gateway/    # domain は外側へ依存しない
```

禁止:
- `domain/` への Spring / JPA / MyBatis など framework アノテーション、インフラロジック
- Saga ステートマシンと `@GlobalTransactional` の併用
- `web/` 層への業務ロジック、`application/` 層からの DB 直接アクセス

必須:
- domain エンティティは純粋な POJO
- gateway インターフェースは `domain/` または `application/` に定義し、`gateway/` で実装
- MyBatis-Plus は `LambdaQueryWrapper` を使う
- DTO / ドメインイベントは `record`（Java 21）
- Webhook は多重到達する前提で冪等に設計する

## ドメイン固有の注意点

- 注文状態遷移: `PENDING → WAITING_PAYMENT → PAID/FAILED`。`PAID` は終端で、遅延到着した失敗イベントは無視する
- Saga 定義: `apps/services/order-service/src/main/resources/statelang/*.json`
- 決済同期: Webhook（署名検証あり）が主、`WAITING_PAYMENT` 注文のポーリングがフォールバック。`payment_last_event_id` で重複処理を防ぐ
- alert-service（Kafka Streams）の整合性ルール: A=決済成功だが注文未更新 / B=注文 PAID だが決済失敗 / C=同一 orderId の重複決済
- テストは `*Test` / `*IntegrationTest` / `*UnitTest` 命名。Saga・決済の変更では正常系・冪等性・補償/失敗系をカバーする
- コミットは Conventional Commits にモジュール名スコープを付ける（例: `feat(search): ...`）

## Docker / ポート

コンテナ間通信は **コンテナ名 + 内部ポート**、ホストからは **外部ポート**。

| サービス | 内部 | 外部 | 環境変数 |
|---|---:|---:|---|
| BFF | 8080 | 18080 | - |
| order-service | 8082 | 18081 | `ORDER_SERVICE_BASE_URL` |
| storage-service | 8083 | 18082 | `STORAGE_SERVICE_BASE_URL` |
| account-service | 8083 | 18083 | `ACCOUNT_SERVICE_BASE_URL` |
| payment-service | 8084 | 18090 | `PAYMENT_SERVICE_BASE_URL` |
| es-service | 8086 | 8086 | `ES_SERVICE_BASE_URL` |

よくある誤り:
- ❌ コンテナ間通信に `localhost` を使う（→ コンテナ名）
- ❌ コンテナ間通信に外部ポートを使う（→ 内部ポート）
- ❌ `application.yml` のポートと docker-compose のマッピング不一致

設定ファイルの置き場所（BASEPATH ルール）:
- ミドルウェアの設定ファイルはリポジトリではなく `BASEPATH` 配下に置く（既定: `/Users/{user-name}/Dev/_Env/_demo/seata-mode`）
- テンプレートは `platform/docker/demo/conf/` に置き、`./setup-env.sh` で BASEPATH へコピーする
- ミドルウェアを追加したら、conf テンプレート追加 → `setup-env.sh` 更新 → compose の volume 追加 をセットで行う
- ランタイムデータや環境固有設定はコミットしない

デモ環境の初期化（ES インデックス / MinIO 画像）:

```bash
cd platform/docker/demo/elasticsearch
./init-es-products-index.sh      # products_v1 作成 + alias + CSV インポート
./init-upload-product-minio.sh   # バケット ec-demo 作成 + product/images/ へアップロード
```

※ OpenSearch は外部コンテナ `smart-dx-opensearch` に依存する。

起動順序・全ポート一覧・nginx ルーティング・ヘルスチェックの詳細は `docs/runbook/README_LOCAL_SETUP.md` を参照。

## セキュリティ

- `.env` と `apps/bff/src/main/resources/serviceAccountKey.json` は絶対にコミットしない

## 参照ドキュメント

- `docs/adr/` — 設計判断（アーキテクチャ変更前に必読）
- `docs/architecture/README_ARCHITECTURE.md` — 全体設計
- `docs/runbook/README_LOCAL_SETUP.md` — ローカル/デモ環境の起動手順
- `docs/README.md` / `docs/docs-policy.md` — ドキュメント正本と編集ルール（feature ブランチから `architecture/` `adr/` `runbook/` を直接編集しない）
