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

### MongoDBの柔軟性に関する補足資料

#### スキーマレス設計のメリット
- ドキュメントごとに異なるフィールド構造を持てる
- 新しいフィールド追加時にマイグレーション不要
- 将来的な拡張に対応しやすい

#### 配列フィールドの柔軟性
- `history`配列に異なる種類のイベントを格納可能
- 配列要素に対してインデックスやクエリが可能
- 後から新しいフィールドを追加してもスキーマ変更不要

#### RDBとの違い
- **RDB**: 新カラム追加時にALTER TABLEが必要、既存データへの影響大
- **MongoDB**: ドキュメント単位で自由に構造を拡張可能

#### 実装例
```javascript
// 初期の監査ログ
{
  orderId: "ORD-001",
  history: [
    { eventId: "evt1", status: "CREATED", timestamp: ISODate(...) }
  ]
}

// 後で新しいフィールドを追加しても問題なし
{
  orderId: "ORD-002",
  history: [
    {
      eventId: "evt2",
      status: "PAID",
      timestamp: ISODate(...),
      paymentMethod: "CREDIT_CARD",  // 新フィールド
      userId: "user123"               // 新フィールド
    }
  ]
}
```

この柔軟性により、長期的な監査ログ管理において拡張性とメンテナンス性が向上します。
