# Title
マイクロサービスにObservability（可観測性）基盤を導入する

## Status (Proposed / Accepted / Deprecated)
Proposed

## Context
- 現在のec-demoはorder-service / payment-service / storage-service / account-service / alert-serviceの5サービス＋BFFで構成されるマイクロサービスアーキテクチャである。
- Seata Sagaによる分散トランザクション、Kafkaによる非同期イベント連携が稼働しているが、**リクエストがどのサービスを経由し、どこで遅延・失敗したかを横断的に追跡する仕組みがない**。
- 障害発生時に各サービスのログを個別に突き合わせる必要があり、MTTR（平均復旧時間）が長くなるリスクがある。
- 運用面で「どこで何が起きたか」を即座に把握できる可観測性は、マイクロサービスにおいて最も重要な非機能要件の一つである。
- Spring Boot 3.x系では Micrometer Tracing がファーストクラスサポートされており、OpenTelemetryとのブリッジも公式に提供されている。

## Decision

### 1. 分散トレーシング: Micrometer Tracing + OpenTelemetry

- **Micrometer Tracing**（旧 Spring Cloud Sleuth）を全サービスに導入し、Trace ID / Span IDの自動伝播を実現する。
- バックエンドエクスポーターとして**OpenTelemetry Protocol (OTLP)**を使用し、トレースデータを収集する。
- Trace IDはHTTPヘッダー（`traceparent` / W3C Trace Context）とKafka Headerで自動伝播する。

### 2. トレース可視化: Jaeger（または Zipkin）

- トレース可視化UIとして**Jaeger**を採用する（開発/ステージング環境）。
  - Jaeger All-in-oneをDocker Composeで構成し、OTLPレシーバーでトレースを受信する。
- Jaegerを選定する理由：
  - OpenTelemetry nativeなサポート（OTLPレシーバー内蔵）。
  - サービス依存グラフの自動生成機能。
  - 高いスケーラビリティ（本番ではElasticsearchバックエンド対応）。

### 3. メトリクス: Micrometer + Prometheus

- **Micrometer**で各サービスのメトリクス（HTTPリクエスト、JVM、Kafkaコンシューマーラグ等）を収集する。
- **Prometheus**でスクレイピングし、**Grafana**で可視化する。
- カスタムメトリクス例：
  - `saga.execution.duration` — Saga実行時間（正常/補償別）
  - `payment.webhook.latency` — Webhook到達遅延
  - `alert.rule.fired.count` — アラートルール発火数

### 4. 構造化ログと相関ID

- 全サービスのログフォーマットをJSON構造化ログに統一する。
- Micrometer Tracingが付与するTrace ID / Span IDをログのMDCに自動挿入する。
- ログ → トレース → メトリクスの三位一体で横断的な分析を可能にする。

### 5. 対象範囲と導入順序

| Phase | 対象 | 内容 |
|---|---|---|
| Phase 1 | 全サービス | Micrometer Tracing導入、Trace ID伝播、JSON構造化ログ |
| Phase 2 | 全サービス | Jaeger + Prometheus + Grafana構築（docker-compose追加） |
| Phase 3 | order / payment | カスタムSpan追加（Saga実行、PayPay API呼出、Webhook処理） |
| Phase 4 | alert-service | Kafka Streams処理のトレース連携 |

## Alternatives

### A. Zipkin（分散トレーシング）
- Spring Bootとの統合が歴史的に強く、導入が容易。
- ただしOpenTelemetry nativeでない点、UIの機能がJaegerに比べ限定的。
- **不採用理由**: OTLPネイティブ対応とサービス依存グラフの観点でJaegerが優位。

### B. Datadog / New Relic（SaaS APM）
- フルマネージドで運用負荷が低い。
- **不採用理由**: デモ/学習プロジェクトのため、OSSスタックでコスト/学習効果の両立を優先。本番移行時の選択肢として残す。

### C. OpenTelemetry Java Agent（自動計装のみ）
- バイトコード計装で変更不要。
- **不採用理由**: Micrometer Tracingの方がSpring Boot Actuatorとの統合が深く、カスタムSpan追加やメトリクスとの連携が容易。ただしJava Agentの併用は将来的に検討可能。

### D. トレーシング未導入（現状維持）
- 導入コストなし。
- **不採用理由**: サービス数が5+BFFあり、Saga/Kafka連携における障害切り分けが困難。運用能力の証明としても可観測性は必須。

## Consequences

### メリット
- order-service → payment-service → PayPay → Webhook → Saga補償の**全フローがTrace IDで一本の線として可視化**される。
- 障害発生時のMTTRが大幅に短縮され、「どのサービスの何番目のSpanで遅延/失敗したか」が即座に判明する。
- メトリクスとログがTrace IDで相関し、三位一体のObservabilityを実現する。
- 技術ポートフォリオとして、OpenTelemetry/Jaeger/Prometheus/Grafanaの実運用経験を証明できる。

### デメリット・注意点
- docker-composeにJaeger / Prometheus / Grafanaが追加され、開発環境のリソース消費が増加する。
- トレースデータの保持期間とストレージ管理が必要（開発環境はインメモリ/短期保持で対応）。
- Kafka Streams（alert-service）のトレース伝播はカスタム実装が必要な場合がある。
- 全サービスへの依存追加が必要だが、Spring Boot Starterのため設定変更主体で実装負荷は低い。

## References

### 公式ドキュメント
- [Micrometer Tracing](https://micrometer.io/docs/tracing) — Spring Boot 3.x公式トレーシングライブラリ
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/) — OTLPエクスポーター仕様
- [Jaeger](https://www.jaegertracing.io/docs/) — 分散トレーシングバックエンド
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/actuator.html) — メトリクス/ヘルスチェック

### 構成イメージ

```
                        ┌───────────────┐
                        │    Grafana     │ ← ダッシュボード
                        └──────┬────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
         ┌──────▼──────┐ ┌────▼─────┐ ┌──────▼──────┐
         │  Prometheus  │ │  Jaeger  │ │   Loki      │
         │ (メトリクス) │ │(トレース)│ │  (ログ)     │
         └──────▲──────┘ └────▲─────┘ └──────▲──────┘
                │              │              │
    ┌───────────┼──────────────┼──────────────┤
    │           │              │              │
┌───▼───┐  ┌───▼───┐   ┌──────▼──────┐  ┌───▼───┐
│ BFF   │  │order  │   │  payment    │  │alert  │
│       │  │service│   │  service    │  │service│
└───────┘  └───────┘   └─────────────┘  └───────┘
     Micrometer Tracing + OTLP Exporter (全サービス共通)
```

### Spring Boot設定例（application.yml）

```yaml
management:
  tracing:
    sampling:
      probability: 1.0        # 開発環境は全量、本番は0.1等
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  metrics:
    tags:
      application: ${spring.application.name}

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Maven依存関係（各サービス共通）

```xml
<!-- Micrometer Tracing + OpenTelemetry Bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Prometheus Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```
