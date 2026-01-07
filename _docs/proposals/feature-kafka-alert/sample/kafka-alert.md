以下は `hello-mid` Kafka デモへ組み込むための「kafka-alert（PoC）」実装プロンプトです。  
目的: Kafka Streams を用いて Rule A/B/C（注文と決済の整合性崩れ）を検知し、`alerts.order_payment_inconsistency.v1` トピックへ `AlertRaised` を出力できる PoC を作成する。

--- 
0) PoC 要件（要約）
- Rule A: `PaymentSucceeded` を受信 → 同一 `orderId` の `OrderConfirmed` が T_confirm 内に来なければ `AlertRaised`
- Rule B: `OrderConfirmed` を受信 → 同一 `orderId` の `PaymentSucceeded` が T_pay 内に来なければ `AlertRaised`
- Rule C: 同一 `orderId` で `PaymentSucceeded` が複数回 → 二重決済疑い `AlertRaised`（重大度 `P1`）
- PoC の簡素化:
  - JSON（String serializer）で OK。Schema Registry / Avro は使わない
  - 高度な deadline bucket 化や AlertResolved は不要
  - 重複抑止は `fired` フラグで良し。`eventId` dedup は任意

1) 推奨構成（最小差分）
- 推奨: 3 アプリ構成（ただし既存構成に合わせて一体化しても可）
  - `order-service` (既存の `order-service` をイベント供給元として利用 → `orders.events.v1` produce)
  - `payment-service` (既存の `payment-service` をイベント供給元として利用 → `payments.events.v1` produce)
  - `alert-streams-service` (Spring Boot, Kafka Streams → `alerts.order_payment_inconsistency.v1` produce)
- topics:
  - `orders.events.v1`
  - `payments.events.v1`
  - `alerts.order_payment_inconsistency.v1`
- 全ての message key = `orderId`

2) イベント形式（JSON）
- OrderConfirmed:
  {
    "eventType":"OrderConfirmed",
    "eventId":"<uuid>",
    "occurredAt":"<ISO8601>",
    "orderId":"O-1001"
  }
- PaymentSucceeded:
  {
    "eventType":"PaymentSucceeded",
    "eventId":"<uuid>",
    "occurredAt":"<ISO8601>",
    "orderId":"O-1001",
    "paymentId":"P-9001",
    "provider":"PayPay",
    "amount":1200,
    "currency":"JPY"
  }
- AlertRaised (出力):
  {
    "eventType":"AlertRaised",
    "alertId":"<uuid>",
    "rule":"A|B|C",
    "severity":"P1|P2",
    "orderId":"O-1001",
    "detectedAt":"<ISO8601>",
    "facts":{
      "orderConfirmedAt":"<ISO8601|null>",
      "paymentSucceededAt":"<ISO8601|null>",
      "paymentSuccessCount":1
    }
  }

3) Kafka Streams 実装（高レベル）
- 入力ストリーム:
  - KStream<String,String> orders = builder.stream("orders.events.v1")
  - KStream<String,String> payments = builder.stream("payments.events.v1")
- 各メッセージを JSON → UnifiedEvent にマッピング（eventType, orderId, occurredAt, ...）
- merge streams → 単一 KStream を Transformer に渡す
- Transformer + StateStore (RocksDB / KeyValueStore) で `OrderPaymentState` を管理
- Punctuator（10s or 30s）で全 state を scan して期限超過を検出しアラートを emit
- Alerts は `alerts.order_payment_inconsistency.v1` に produce（key=orderId）

4) State 定義（orderId 単位）
- OrderPaymentState:
  - orderConfirmedAtEpochMs: Long? 
  - paymentSucceededAtEpochMs: Long?
  - paymentSuccessCount: int
  - ruleADeadlineEpochMs: Long?  (paymentSucceededAt + T_confirm)
  - ruleBDeadlineEpochMs: Long?  (orderConfirmedAt + T_pay)
  - ruleAFired: boolean
  - ruleBFired: boolean
  - ruleCFired: boolean

5) ルールロジック（Transformer 内）
- on PaymentSucceeded:
  - state.paymentSuccessCount++
  - if paymentSucceededAt not set: set to occurredAt
  - if paymentSuccessCount >= 2 && !ruleCFired: emit Alert(rule=C,severity=P1); ruleCFired=true
  - if orderConfirmedAt is null: ruleADeadline = paymentSucceededAt + T_confirm
- on OrderConfirmed:
  - if orderConfirmedAt not set: set to occurredAt
  - if paymentSucceededAt is null: ruleBDeadline = orderConfirmedAt + T_pay
- punctuate (every 10s/30s):
  - for each state:
    - if ruleADeadline != null && now >= ruleADeadline && orderConfirmedAt==null && !ruleAFired:
      - emit Alert(rule=A,severity=P2); ruleAFired=true
    - if ruleBDeadline != null && now >= ruleBDeadline && paymentSucceededAt==null && !ruleBFired:
      - emit Alert(rule=B,severity=P2); ruleBFired=true

6) 設定値（PoC）
- T_confirm: 30s（テスト時は 5〜30s に短縮可）
- T_pay: 30s（同上）
- punctuate interval: 10s

7) Simulator REST API（既存サービスを利用する方針）
- 既存の `order-service` / `payment-service` がイベントを publish することを想定します。独立したシミュレータは必須ではありません。
- 参考（もし独立シミュレータを用意する場合のエンドポイント例）:
 - POST /sim/order/confirmed
   - req: { "orderId":"O-1", "occurredAt": "optional" }
   - サーバが eventId/occurredAt を補完して `orders.events.v1` へ produce
 - POST /sim/payment/succeeded
   - req: { "orderId":"O-1", "paymentId":"P-1", "provider":"PayPay", "amount":1200, "currency":"JPY", "occurredAt":"optional" }
   - サーバが eventId/occurredAt を補完して `payments.events.v1` へ produce
 - 便利なシナリオ（参考）:
   - POST /sim/scenario/send-payment-succeeded  (PaymentSucceeded を1回送る)
   - POST /sim/scenario/send-order-confirmed  (OrderConfirmed を1回送る)
   - POST /sim/scenario/send-payment-succeeded-twice  (PaymentSucceeded を2回送る)

8) ローカル起動（docker-compose）
- Redpanda or Kafka を docker-compose で起動（既存 compose を拡張）
- topics を起動時に作成（docker-compose の init または起動後スクリプト）
- 各 Spring Boot の `application.yml` で bootstrap-servers を統一

9) テスト手順（curl 例）
- Rule C (即時):
  curl -X POST http://localhost:8082/sim/payment/succeeded -H 'Content-Type: application/json' -d '{"orderId":"O-3","paymentId":"P-1","provider":"PayPay","amount":1200,"currency":"JPY"}'
  curl -X POST http://localhost:8082/sim/payment/succeeded -H 'Content-Type: application/json' -d '{"orderId":"O-3","paymentId":"P-2","provider":"PayPay","amount":1200,"currency":"JPY"}'
- Rule A (期限超過):
  curl -X POST http://localhost:8082/sim/payment/succeeded -H 'Content-Type: application/json' -d '{"orderId":"O-1","paymentId":"P-9","provider":"PayPay","amount":1200,"currency":"JPY"}'
  wait T_confirm + punctuate interval → alerts topic に AlertRaised(rule=A)
- Rule B (期限超過):
  curl -X POST http://localhost:8081/sim/order/confirmed -H 'Content-Type: application/json' -d '{"orderId":"O-2"}'
  wait T_pay + punctuate interval → alerts topic に AlertRaised(rule=B)

10) 実装上の注意点
- Key は必ず `orderId`。Streams の `Produced.with(Serdes.String(), Serdes.String())` を想定
- JSON parse 失敗はログして無視
- StateStore はスレッドセーフに、Store のマイグレーションや TTL は PoC では無視
- ログには `orderId`, `rule`, `deadline`, `now` を含める

11) 期待成果物（チェックリスト）
- `kafka-alert-streams` モジュール（Spring Boot + Kafka Streams）: Topology, Transformer, StateStore, Punctuator 実装
- simulator(s): REST API から `orders.events.v1` / `payments.events.v1` へ送信できるもの
- docker-compose: Kafka/Redpanda と topic 作成
- README: 起動手順、curl シナリオ、topics 一覧

--- 
出力指示（このファイルは実装者へ渡すプロンプトとして使う）
- 「実装方針とモジュール構成案」を最初に提示すること
- 次に必要なファイル/差分（Topology, Transformer クラス, State クラス, REST controller, docker-compose 追記）を列挙して示すこと
- 最後に README の起動・テスト手順（curl 例）を完成させること

この内容でプロンプトを更新しました。次は「Code関連の差分（Topology, Transformer, State class, sample controller）」を列挙してファイル差分を作成しますか?
