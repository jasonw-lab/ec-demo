# Title
Resilience4j でサービス間通信のレジリエンス（回復性）を強化する

## Status (Proposed / Accepted / Deprecated)
Proposed

## Context
- 現在のec-demoはSeata Sagaで分散トランザクションの整合性を管理しているが、**サービス間のネットワーク遅延・タイムアウト・一時障害への耐性が不足**している。
- order-serviceからpayment-serviceへの同期呼び出し、BFFから各サービスへのAPI呼び出しなど、複数のサービス間通信箇所がある。
- 各サービスのHTTPクライアントはSpring標準の `RestTemplate`（`RestTemplateBuilder` 経由）で実装されており、Seata XIDや `X-Request-Id` の伝播Interceptorも設定済みである。
- 外部決済（PayPay API）は自システムの制御外であり、応答遅延やダウンが発生し得る。なお、PayPay APIの呼び出しにはPayPay公式Java SDK（`jp.ne.paypay.ApiClient`）を利用しており、直接HTTPクライアントを操作しない設計になっている。
- 一つのサービス障害が連鎖してシステム全体を停止させる「カスケード障害」を防ぐ設計が必要である。
- Spring Boot 3.x + Java 21環境でResilience4jは公式にサポートされており、Micrometer連携も標準対応している。

## Decision

### 1. Circuit Breaker（サーキットブレーカー）

外部サービスが障害状態にあるとき、リクエストを即座にフェイルさせ、カスケード障害を防止する。

#### 適用箇所

| 呼び出し元 | 呼び出し先 | 理由 |
|---|---|---|
| order-service | payment-service | 決済サービス障害時に注文処理全体を保護 |
| order-service | storage-service | 在庫サービス障害時にSaga全体を保護 |
| payment-service | PayPay API | 外部決済の障害・遅延を遮断 |
| BFF | 各 service | フロントエンドへの影響を局所化 |

#### 設定方針

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10           # 直近10リクエストで判定
        failure-rate-threshold: 50        # 50%以上失敗でOPEN
        wait-duration-in-open-state: 30s  # 30秒後にHALF-OPEN
        permitted-number-of-calls-in-half-open-state: 3
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.ResourceAccessException
    instances:
      paymentService:
        base-config: default
      storageService:
        base-config: default
      payPayApi:
        base-config: default
        failure-rate-threshold: 40        # 外部APIはより厳しく
        wait-duration-in-open-state: 60s  # 外部APIは復旧に時間がかかる
```

#### 状態遷移

```
  CLOSED ──(失敗率 ≥ 閾値)──→ OPEN ──(待機時間経過)──→ HALF-OPEN
    ▲                                                      │
    └──────(成功率が十分)──────────────────────────────────┘
                          │
                          └──(失敗継続)──→ OPEN に戻る
```

### 2. Retry（リトライ）

一時的な障害（ネットワーク瞬断、一時的な503）に対して自動リトライで回復を試みる。

#### 適用箇所と設定

```yaml
resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2  # 500ms → 1s → 2s
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.demo.ec.common.exception.BusinessException  # ビジネスエラーはリトライしない
    instances:
      paymentService:
        base-config: default
      storageService:
        base-config: default
        max-attempts: 2                    # 在庫操作は冪等性に注意
```

#### リトライ対象の判断基準

| 条件 | リトライ | 理由 |
|---|---|---|
| ネットワークタイムアウト | ✅ | 一時的な遅延の可能性が高い |
| 503 Service Unavailable | ✅ | サービス再起動中の可能性 |
| 400 Bad Request | ❌ | リクエスト自体が不正、リトライしても同結果 |
| 409 Conflict | ❌ | ビジネスロジック上の競合、リトライで解消しない |
| 決済作成リクエスト | ⚠️ 条件付き | 冪等キー付きの場合のみリトライ可 |

### 3. TimeLimiter（タイムアウト制御）

外部呼び出しに上限時間を設定し、無制限の待機を防止する。

```yaml
resilience4j:
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 5s
      payPayApi:
        timeout-duration: 10s             # 外部APIは余裕を持つ
      storageService:
        timeout-duration: 3s
```

### 4. Bulkhead（バルクヘッド）

同時実行数を制限し、一つのサービスへの呼び出しがスレッドプールを枯渇させることを防ぐ。

```yaml
resilience4j:
  bulkhead:
    instances:
      payPayApi:
        max-concurrent-calls: 20          # PayPay APIへの同時呼び出し上限
        max-wait-duration: 500ms
```

### 5. Fallback（フォールバック戦略）

Circuit BreakerがOPENの際に、ユーザー体験を維持するためのフォールバック応答を定義する。

| サービス | Fallback戦略 |
|---|---|
| payment-service障害 | 注文を「決済保留（PAYMENT_PENDING）」で受け付け、後続処理はSagaのタイムアウトで補償 |
| storage-service障害 | 注文受付を一時停止し、503 + `Retry-After`ヘッダーで再試行を促す |
| PayPay API障害 | ポーリングにフォールバックし、Webhook復旧後に再開 |
| BFF → service障害 | サービス別の縮退レスポンスを返し、UI側で該当機能を無効化 |

### 6. Seataとの統合方針

- Resilience4jはSagaの**内側**（各Sagaステップのサービス呼び出し）に適用する。
- Circuit BreakerがOPENの場合、Sagaステップは即座に失敗し、Saga Orchestratorが補償トランザクションを起動する。
- リトライはSagaステップの**内部**で完結させ、Sagaレベルのリトライとは独立に管理する。

```
Saga Orchestrator (Seata)
  │
  ├─ Step1: 在庫引当
  │    └─ Resilience4j [Retry → CircuitBreaker → TimeLimiter] → storage-service
  │
  ├─ Step2: 決済要求
  │    └─ Resilience4j [Retry → CircuitBreaker → TimeLimiter] → payment-service
  │         └─ Resilience4j [Retry → CircuitBreaker → Bulkhead] → PayPay API
  │
  └─ 失敗時: 補償トランザクション（在庫解放・注文キャンセル）
```

### 7. Observabilityとの連携（ADR-105参照）

- Resilience4jの全イベント（状態遷移、リトライ、タイムアウト）をMicrometerメトリクスとして自動公開する。
- Circuit Breaker状態は `resilience4j.circuitbreaker.state` メトリクスでPrometheus/Grafanaに反映される。
- Grafanaダッシュボードで以下を可視化する：
  - Circuit Breaker状態（CLOSED / OPEN / HALF-OPEN）の時系列
  - リトライ回数とバックオフ分布
  - フォールバック発動回数

## Alternatives

### A. Netflix Hystrix
- 長い実績があるが、**メンテナンスモード（2018年以降更新なし）**。
- **不採用理由**: 新規プロジェクトでの採用は非推奨。Spring Boot 3.xとの互換性も保証されない。

### B. Spring Cloud Circuit Breaker（抽象レイヤー）
- Resilience4jのラッパーとして機能する。
- **不採用理由**: 抽象化層を通すことで設定の柔軟性が低下する。Resilience4jを直接使う方がきめ細かい制御が可能。

### C. Istio / Envoy によるサービスメッシュレベルのリトライ・サーキットブレーカー
- インフラ層で透過的に適用できる。
- **不採用理由**: 現時点ではKubernetes不要のdocker-compose構成であり、サービスメッシュは過剰。将来のK8s移行時に検討する。

### D. レジリエンス未導入（現状維持）
- 導入コストなし。
- **不採用理由**: PayPay APIの障害やサービス間のネットワーク遅延でカスケード障害が発生するリスクがある。「外部サービスが死んでいるときにシステム全体を落とさない設計」は運用能力の証明として必須。

## Consequences

### メリット
- PayPay APIやサービス間通信の障害が**局所化**され、システム全体のダウンを防止する。
- Circuit Breaker / Retry / TimeLimiterの組み合わせにより、**一時的な障害は自動回復**し、恒久的な障害は素早く遮断される。
- Seata Sagaと統合することで、「リトライで回復しない場合は補償トランザクションで整合性を保つ」という**二段階の回復戦略**が実現する。
- Micrometerメトリクスとの連携により、回復性の状態がGrafanaで可視化され、運用判断が迅速になる。
- 技術ポートフォリオとして、Resilience4j + Circuit Breaker + Sagaの統合実装を証明できる。

### デメリット・注意点
- **冪等性の確保が必須**: リトライ対象のAPI（特に決済作成）は冪等キーの実装が前提となる。
- **設定の調整が必要**: 閾値（失敗率、待機時間、リトライ回数）は実測に基づくチューニングが必要。
- **テストの複雑化**: Circuit BreakerがOPEN状態のときのSaga補償フロー等、異常系のテストシナリオが増加する。
- **依存関係追加**: 全サービスにResilience4j Starterの依存が追加される（全サービス共通だが、Spring Boot Starterのため設定変更主体で実装負荷は低い）。
- **RestTemplateとの相性（好材料）**: 既存コードは `RestTemplateBuilder` 経由でBean構築済みであるため、Micrometer Tracer（ADR-105）が自動的にInterceptorを注入し、Trace IDの伝播もスムーズに行える。また、Seata XIDの伝播Interceptorと共存する形で Resilience4j のデコレーションを重ねられる。

## References

### 公式ドキュメント
- [Resilience4j 公式](https://resilience4j.readme.io/) — CircuitBreaker, Retry, TimeLimiter, Bulkhead
- [Resilience4j Spring Boot 3 Starter](https://resilience4j.readme.io/docs/getting-started-3) — Spring Boot統合
- [Micrometer × Resilience4j](https://resilience4j.readme.io/docs/micrometer) — メトリクス連携

### Maven依存関係

```xml
<!-- Resilience4j Spring Boot 3 Starter -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- AOP (アノテーション駆動に必要) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 実装例1: order-service → payment-service（`RestTemplate`ベースのクライアント）

現在の実装は `RestTemplate`（`RestTemplateBuilder` 経由）を使用しています。Resilience4jのアノテーションはメソッドに付与します。

```java
// order-service: PaymentClient.java に Circuit Breaker / Retry を追加する例
@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentClient(RestTemplate restTemplate,
                         @Value("${svc.payment.baseUrl:http://localhost:8090}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentResult requestPayment(String orderNo, BigDecimal amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Order-No", orderNo);
        Map<String, Object> body = Map.of("orderNo", orderNo, "amount", amount);
        ResponseEntity<PaymentResult> response = restTemplate.postForEntity(
                paymentBaseUrl + "/internal/payment/paypay/pay",
                new HttpEntity<>(body, headers),
                PaymentResult.class
        );
        return bodyOrFailure(response, orderNo);
    }

    // Circuit Breaker OPEN時のフォールバック: 即座にPENDINGを返しSagaの補償へ
    private PaymentResult requestPaymentFallback(String orderNo, BigDecimal amount, Throwable t) {
        log.warn("PaymentClient CB open or retry exhausted orderNo={} err={}", orderNo, t.getMessage());
        PaymentResult result = new PaymentResult();
        result.setSuccess(false);
        result.setCode("CB_OPEN");
        result.setMessage("payment-service unavailable: " + t.getMessage());
        result.setOrderNo(orderNo);
        return result;
    }
}
```

### 実装例2: payment-service → PayPay API（公式Java SDKの場合）

PayPay APIはSpringのHTTPクライアント直接ではなく**PayPay公式Java SDK**（`jp.ne.paypay.ApiClient`）経由で呼び出すため、SDKのHTTP層はResilience4jの自動Proxyの対象外となります。そのため、**SDKを呼び出すServiceメソッドのレイヤー**（`PaypayPaymentServiceImpl`）に対してアノテーションを付与します。

```java
// payment-service: PaypayPaymentServiceImpl.java に Circuit Breaker / Retry を追加する例
@Service
public class PaypayPaymentServiceImpl implements PaymentService {

    // ... 既存フィールド ...

    @Override
    @CircuitBreaker(name = "payPayApi", fallbackMethod = "createPaymentSessionFallback")
    @Retry(name = "payPayApi")
    public PaymentSession createPaymentSession(String merchantPaymentId, BigDecimal amountJPY,
                                               Map<String, Object> metadata) {
        // 既存のSDK呼び出しロジックをそのままラップ
        QRCodeDetails details = createQrCode(merchantPaymentId, amountJPY, metadata);
        return toPaymentSession(merchantPaymentId, details);
    }

    private PaymentSession createPaymentSessionFallback(String merchantPaymentId, BigDecimal amountJPY,
                                                        Map<String, Object> metadata, Throwable t) {
        log.warn("PayPay SDK CB open orderNo={} cause={}", merchantPaymentId, t.getMessage());
        // フォールバック: 呼び出し元（InternalPaypayController）へ例外を伝播し
        // Saga Orchestratorがcompensateへ遷移する
        throw new PayPayApiUnavailableException("PayPay API unavailable", t);
    }
}
```

> **ポイント**: SDKの `ApiClient` は内部でHTTPを発行するが、SpringのBean管理外のため`@CircuitBreaker`でラップするにはService層（`PaypayPaymentServiceImpl`のpublicメソッド）を対象にするのが最も実装負荷が低い。

### Actuatorで確認できるエンドポイント

```
GET /actuator/circuitbreakers         — 全Circuit Breakerの状態一覧
GET /actuator/circuitbreakerevents    — 状態遷移イベント履歴
GET /actuator/retries                 — Retry設定一覧
GET /actuator/retryevents             — リトライイベント履歴
```
