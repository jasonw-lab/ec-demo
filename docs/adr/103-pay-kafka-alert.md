# Title
Kafka Streamsで注文・決済の不整合アラートを検知する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- 注文・決済は非同期に状態が変化し、イベント到達順序や欠落で一時的な不整合が起こり得る。
- 不整合の早期検知と運用復旧を加速する仕組みが必要。
- 既存サービスへの影響を最小化し、追加サービスで完結させたい。

## Decision
- alert-serviceを追加し、Kafka Streamsで注文/決済イベントを相関させて検知する。
- ルールはRule A/B/C（注文未反映、決済不成立、二重決済）を実装し、StateStoreでorderId単位の状態を保持する。
- 検知結果は`AlertRaised`として`ec-demo.alerts.order_payment_inconsistency.v1`へ送信し、MySQLにUPSERTで保存する。

## Alternatives
- バッチ整合性チェック（検知遅延が大きい）。
- 同期API内で検証（外部イベント遅延に弱く、可用性が下がる）。
- Streamsを使わず単純Consumerで実装（期限監視・重複抑止が複雑になる）。

## Consequences
- ほぼリアルタイムで不整合を検知でき、運用判断（再送・補償・返金）が早くなる。
- Kafka StreamsのStateStoreとPunctuatorの運用が必要になる。
- at-least-once配信に備え、重複抑止と冪等な処理設計が必須になる。

## References

