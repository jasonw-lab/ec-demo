# Title
決済ドメインをマイクロサービスとして分割する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- フロントとバックエンドの間にBFFを置き、注文・決済・在庫・アカウント・アラートが連携する構成を前提としている。
- 外部決済（PayPay）や非同期イベント（Kafka）を扱い、ドメインごとに責務とスケール要件が異なる。
- 各サービスが独立して変更・スケールできること、障害影響を局所化できることを重視する。

## Decision
- BFF + 複数マイクロサービス（order-service / payment-service / storage-service / account-service / alert-service）で構成する。
- 各サービスは自分のデータストアを所有し、同期APIは最小限、非同期イベントはKafkaで連携する。
- サービス間の一貫性はSaga（Seata）で最終整合性を担保する。

## Alternatives
- 単一モノリス（実装は容易だが、障害影響とスケール要件が混在する）。
- モジュラーモノリス（内部の分離は可能だが、デプロイや障害隔離が弱い）。


## Consequences
- 独立したスケールやデプロイが可能になり、外部決済や監査などの関心事を分離できる。
- 反面、分散トランザクションや観測性（ログ/トレース/相関ID）の設計が必須になる。
- Kafka、Seata、Redisなどの運用コンポーネントが増える。

## References

