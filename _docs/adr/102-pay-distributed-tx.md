# Title
決済系の分散トランザクションにSaga（Seata Orchestration）を採用する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- 注文・在庫・決済が別サービスであり、サービス間でACIDトランザクションは難しい。
- 外部決済（PayPay）はトランザクション制御外で、成功/失敗の遅延や不確実性がある。
- 一時的な不整合を許容しつつ、最終整合性に到達できる仕組みが必要。

## Decision
- Saga Pattern（Orchestration型）を採用し、SeataのSagaエンジンで状態遷移を管理する。
- order-serviceでSagaを起動し、在庫引当→決済要求→成功/失敗の補償を状態機械で表現する。
- 補償トランザクション（在庫解放・注文キャンセル）を定義し、失敗時に自動実行する。

## Alternatives
- 2PC（強い整合性は得られるが、可用性と性能が犠牲になる）。
- Saga Choreography（SPOFは避けられるが、全体の状態把握が難しい）。
- Best Effort / リトライのみ（決済の整合性要件に不足）。

## Consequences
- 最終整合性となるため、注文ステータスの遅延反映が発生し得る。
- Orchestratorに障害があると復旧が遅れるため、監視と運用手順が必要。
- 補償処理は冪等である必要があり、ログ・相関IDの設計が重要になる。

## References
