# Title
注文監査ログをMongoDBで管理する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- 注文の正データはRDBで管理し、状態遷移の履歴は監査・説明責任のために長期保存したい。
- 監査ログはスキーマ変更に強く、履歴配列を柔軟に持てるストアが必要。
- 監査は補助的データであり、強い整合性よりも可観測性と追跡性を重視する。

## Decision
- MongoDBに`order_audit`コレクションを作成し、1注文=1ドキュメントで履歴配列を保持する。
- order-serviceでKafkaの`orders-events`を購読し、`OrderStatusChanged`を監査履歴として追記する。
- 冪等性のため`processedEventIds`を持ち、`history.eventId`にインデックスを作成する。

## Alternatives
- RDBに履歴テーブルを追加（スキーマ変更が重く、履歴モデルが固定化される）。
- EventStoreや専用監査基盤（運用コストが高い）。
- Kafkaのみを証跡として利用（長期検索と一覧取得が難しい）。

## Consequences
- 監査ログは最終整合性となり、数秒〜数分の遅延が発生し得る。
- MongoDBの運用とデータ肥大化対策（アーカイブやTTL等）が必要になる。
- 監査用途に適した柔軟な履歴モデルを維持できる。

## References

