# ECマイクロサービスの技術選定を全公開 — 11のADRで振り返る設計判断　

## はじめに

個人開発のECデモプロジェクトで、決済機能を持つマイクロサービスを構築しました。本記事では、プロジェクトで作成した **11件のADR（Architecture Decision Records）** を通じて、技術選定の背景と判断基準を共有します。

「なぜその技術を選んだのか」「何を諦めたのか」を言語化することで、同様のアーキテクチャを検討する方の参考になれば幸いです。

---

## プロジェクト概要

| 項目 | 内容 |
|-----|------|
| **ドメイン** | EC決済システム（PayPay連携） |
| **構成** | BFF + 6マイクロサービス |
| **技術スタック** | Java 21, Spring Boot 3.2, Vue 3, Seata Saga, Kafka Streams |
| **インフラ** | Docker Compose, MySQL, Redis, MongoDB, Elasticsearch, MinIO |

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend (Vue 3)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      BFF (Spring Boot)                        │
│            Firebase Auth検証 / Redis Session / WebSocket      │
└─────────────────────────────────────────────────────────────┘
          │           │           │           │           │
          ▼           ▼           ▼           ▼           ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
    │ order   │ │ payment │ │ storage │ │ account │ │  alert  │
    │ service │ │ service │ │ service │ │ service │ │ service │
    └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
         │           │           │           │           │
         └───────────┴───────────┴───────────┴───────────┘
                              │
                    ┌─────────┴─────────┐
                    │      Kafka        │
                    └───────────────────┘
```

---

## ADR一覧

| ID | テーマ | ステータス |
|----|-------|-----------|
| 001 | Monorepo + Hybrid Hexagonal | ✅ 採用 |
| 100 | マイクロサービス分割 | ✅ 採用 |
| 102 | Seata Saga（分散トランザクション） | ✅ 採用 |
| 103 | Kafka Streams（整合性アラート） | ✅ 採用 |
| 104 | MongoDB（監査ログ） | ✅ 採用 |
| 105 | Observability基盤 | 📋 提案中 |
| 106 | Resilience4j（レジリエンス） | 📋 提案中 |
| 110 | Firebase Auth | ✅ 採用 |
| 112 | MinIO（画像ストレージ） | ✅ 採用 |

---

## 1. アーキテクチャの土台: Hybrid Hexagonal（ADR-001）

### 課題

初期のフラットなパッケージ構成では、サービス数が増えると以下の問題が発生しました：

- ドメインロジックとインフラ実装が混在
- AI開発ツールが誤った責務のコードを生成
- 技術スタック変更時の影響範囲が広い

### 決定

**Hybrid Hexagonal**を採用。純粋なヘキサゴナルを簡略化し、4層構成としました：

```
web/          → REST API定義、バリデーション（Inbound Adapter）
application/  → ユースケース実行、トランザクション境界
domain/       → ビジネスロジック、POJO（依存ゼロ）
gateway/      → DB操作、外部API呼び出し（Outbound Adapter）
```

**依存ルール**: `web → application → domain ← gateway`。domainは外側に依存しない。

### なぜ「Hybrid」なのか

1. **フラットなパッケージ名**: `adapter/in/web`ではなく`web`。Spring Bootとの親和性を優先
2. **過剰抽象の回避**: DDD本来の`aggregate`/`value object`等の深い構造は採用せず、開発速度を維持

### 採用しなかった選択肢

- **標準的なヘキサゴナル**: 階層が深くなり、小規模チームには過剰
- **レイヤードアーキテクチャ**: ドメイン保護が弱く、Fat Controllerになりがち

---

## 2. なぜマイクロサービスなのか（ADR-100）

### 決定

BFF + 5バックエンドサービス（order/payment/storage/account/alert）で構成。

### 分割の判断基準

| サービス | 分割理由 |
|---------|---------|
| **payment-service** | 外部API（PayPay）依存。障害隔離が必須 |
| **order-service** | Saga Orchestrator。状態機械の複雑性を分離 |
| **storage-service** | 在庫管理。スケール要件が異なる |
| **account-service** | ユーザー/残高管理。認証連携 |
| **alert-service** | Kafka Streams。計算負荷が高い |

### 採用しなかった選択肢

- **モノリス**: 障害影響が全体に波及。スケール粒度が粗い
- **モジュラーモノリス**: 内部分離は可能だが、デプロイ単位が同一。障害隔離が弱い

### トレードオフの認識

マイクロサービス化により以下のコストが発生：
- 分散トランザクション管理（→ Seataで対応）
- 可観測性の複雑化（→ ADR-105で対応予定）
- 運用コンポーネント増加（Kafka, Seata, Redis等）

---

## 3. 分散トランザクション: Seata Saga（ADR-102）

### 課題

注文処理は「在庫引当 → 決済 → 注文確定」の複数サービスをまたぐ。外部決済（PayPay）は制御外であり、ACIDトランザクションは不可能。

### 決定

**Saga Pattern（Orchestration型）** を採用し、Seata Saga Engineで状態遷移を管理。

```
┌─────────────────────────────────────────────────────────┐
│                  Saga State Machine                       │
├─────────────────────────────────────────────────────────┤
│ PENDING → STOCK_RESERVED → PAYMENT_REQUESTED → PAID      │
│     │           │                 │                       │
│     │           │                 └──(失敗)→ COMPENSATING │
│     │           └──(失敗)→ STOCK_RELEASED → CANCELLED     │
│     └──(失敗)→ CANCELLED                                  │
└─────────────────────────────────────────────────────────┘
```

### 補償トランザクションの設計

| ステップ | 正常処理 | 補償処理 |
|---------|---------|---------|
| 在庫引当 | `reserveStock()` | `releaseStock()` |
| 決済要求 | `requestPayment()` | `cancelPayment()` |
| 注文確定 | `confirmOrder()` | `cancelOrder()` |

**冪等性**: 補償処理は複数回呼ばれても同じ結果になるよう設計。`payment_last_event_id`で重複処理を防止。

### 採用しなかった選択肢

| 方式 | 不採用理由 |
|-----|-----------|
| **2PC** | 可用性が犠牲。外部決済では実装不可 |
| **Saga Choreography** | 全体の状態把握が困難。デバッグが難しい |
| **Best Effort** | 決済の整合性要件を満たさない |

---

## 4. リアルタイム整合性検知: Kafka Streams（ADR-103）

### 課題

非同期イベント駆動では、イベント到達順序の入れ替わりや欠落で一時的な不整合が発生する。事後のバッチ検査では検知が遅い。

### 決定

**alert-service**を追加し、Kafka Streamsで注文/決済イベントを相関させてリアルタイム検知。

### 検知ルール

| ルール | 条件 | アクション |
|-------|------|-----------|
| **Rule A** | 決済成功イベントから5分経過しても注文がPAIDにならない | アラート発行 |
| **Rule B** | 注文がPAIDだが、決済イベントがFAILED | アラート発行 |
| **Rule C** | 同一orderIdに複数の決済成功イベント | 二重決済アラート |

### 実装ポイント

- **StateStore**: orderIdごとに状態を保持（注文イベント/決済イベントの到着状況）
- **Punctuator**: 定期的にStateStoreをスキャンし、期限切れをチェック
- **重複抑止**: 同一不整合に対するアラートは一定時間発行しない

### 採用しなかった選択肢

- **バッチ整合性チェック**: 検知遅延が大きく、運用判断が遅れる
- **同期API内で検証**: 外部イベント遅延に弱く、可用性が低下

---

## 5. 監査ログ: MongoDB（ADR-104）

### 課題

注文の正データはRDBで管理するが、状態遷移の履歴は監査・説明責任のために長期保存したい。RDBの履歴テーブルはスキーマ変更が重い。

### 決定

MongoDBに`order_audit`コレクションを作成。1注文 = 1ドキュメントで履歴配列を保持。

```javascript
{
  orderId: "ORD-001",
  processedEventIds: ["evt1", "evt2"],
  history: [
    { eventId: "evt1", status: "CREATED", timestamp: ISODate(...) },
    { eventId: "evt2", status: "PAID", timestamp: ISODate(...), paymentMethod: "PAYPAY" }
  ]
}
```

### MongoDBを選んだ理由

| RDB | MongoDB |
|-----|---------|
| 新カラム追加にALTER TABLE必要 | ドキュメント単位で自由に拡張 |
| 履歴配列の表現が複雑 | ネイティブに配列をサポート |
| トランザクション必須の場面で強い | 最終整合性で十分な監査用途に適合 |

### トレードオフ

- 監査ログは最終整合性（数秒〜数分の遅延あり）
- MongoDBの運用とデータ肥大化対策（TTL等）が必要

---

## 6. 認証: Firebase Auth（ADR-110）

### 課題

デモ実装において、認証機能を短期間で実装し、ビジネスロジックの開発に集中したい。

### 決定

- フロント: Firebase Auth Client SDKでサインイン、ID Token取得
- BFF: Firebase Admin SDKでID Token検証、Redisにセッション発行
- account-service: Firebase UIDと内部ユーザーIDを同期

### 採用しなかった選択肢

| 方式 | 不採用理由 |
|-----|-----------|
| **自前認証** | パスワード管理、MFA、脆弱性対応の運用負荷。実装に数週間〜数ヶ月 |
| **Auth0/Cognito** | 設定が複雑で初期セットアップに時間がかかる |
| **Spring Authorization Server** | OAuth2/OIDC準拠の本格認可サーバーだが、デモにはオーバースペック |

### 正直な評価

Firebaseは「認証をさっさと終わらせてビジネスロジックに集中したい」ケースに最適。本格的なエンタープライズ要件（SAML連携、細かいRBAC等）には別途検討が必要。

---

## 7. 画像ストレージ: MinIO（ADR-112）

### 課題

ローカル開発環境でも本番同様のオブジェクトストレージAPIを使いたい。Elasticsearchには画像URLのみ保持し、検索性能を最適化したい。

### 決定

MinIO（S3互換）を採用。Docker Composeでローカル起動し、将来的なAWS S3移行を容易に。

### 採用しなかった選択肢

| 方式 | 不採用理由 |
|-----|-----------|
| **AWS S3直接利用** | ローカル開発でのセットアップが複雑、AWSアカウント必要 |
| **ローカルファイルシステム** | マイクロサービス間でのファイル共有が困難 |
| **MongoDB GridFS** | HTTPでの直接アクセス不可、配信パフォーマンスが劣る |

---

## 8. 今後の計画: Observability & Resilience（ADR-105, 106）

### Observability（ADR-105）— Proposed

現状の課題:
- リクエストがどのサービスを経由し、どこで遅延・失敗したかを横断的に追跡する仕組みがない
- 各サービスのログを個別に突き合わせる必要があり、MTTR（平均復旧時間）が長くなるリスク

計画:
- Micrometer Tracing + OpenTelemetry + Jaegerで分散トレーシング
- Prometheus + Grafanaでメトリクス可視化
- 構造化ログでTrace ID相関

### Resilience（ADR-106）— Proposed

現状の課題:
- サービス間のネットワーク遅延・タイムアウト・一時障害への耐性が不足
- 一つのサービス障害が連鎖してシステム全体を停止させるリスク

計画:
- Resilience4jでCircuit Breaker / Retry / TimeLimiter / Bulkhead導入
- Seata Sagaとの統合（Sagaステップ内でResilience4jを適用）

---

## まとめ: ADRを書く意義

### 技術選定で重要な3つの観点

1. **Context**: なぜその判断が必要になったのか（課題の言語化）
2. **Decision**: 何を選び、何を選ばなかったのか（Alternativesの明示）
3. **Consequences**: 何を得て、何を諦めたのか（トレードオフの認識）

### ADRの効果

| 効果 | 詳細 |
|-----|------|
| **オンボーディング加速** | 新メンバーが「なぜこうなっているのか」を即座に理解 |
| **意思決定の透明化** | 後から「誰がいつ何を根拠に決めたか」を追跡可能 |
| **再検討の土台** | 状況が変わったときに、当時の判断基準を参照して再評価 |
| **ポートフォリオとしての価値** | 技術選定能力の証明 |

---

## 技術スタック一覧

### バックエンド
- Java 21, Spring Boot 3.2.8
- Seata 2.0 (Saga), Kafka Streams
- MyBatis-Plus 3.5.8, Spring Cloud OpenFeign
- Firebase Admin SDK

### データストア
- MySQL 8.0（トランザクションデータ）
- MongoDB 6.0（監査ログ）
- Redis（セッション）
- Elasticsearch 8.x（商品検索）
- MinIO（画像ストレージ）

### フロントエンド
- Vue 3, TypeScript 5.9, Vite 5.4

### インフラ
- Docker / Docker Compose
- GitHub Actions

---

## リポジトリ

（公開時にURLを追記）

TODO 06/20: リポジトリ公開後にURLを追記
---

*この記事は実際のプロジェクトのADRを元に構成しています。質問やフィードバックがあればコメントをお願いします。*
