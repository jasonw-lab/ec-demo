# Tech Leader ポートフォリオ化 タスク一覧

**目的**: ec-demo リポジトリを「マイクロサービス実績ポートフォリオ」として公開可能な状態にする
**作成日**: 2026-06-20
**対象ブランチ**: techleader/test
**担当**: Kimi

---

## タスク概要

| 優先度 | カテゴリ | タスク数 | 推定工数 |
|-------|---------|---------|---------|
| 🔴 High | テスト拡充 | 4 | 3-5日 |
| 🔴 High | 静的解析導入 | 3 | 1日 |
| 🔴 High | CI品質ゲート | 2 | 0.5日 |
| 🟡 Medium | ドキュメント整備 | 5 | 1-2日 |
| 🟡 Medium | コード整理 | 3 | 0.5日 |
| 🟢 Low | 差別化要素 | 4 | 3-5日 |

---

## 🔴 High Priority（公開前に必須）

### 1. テスト拡充

#### 1.1 テストカバレッジ報告機構の導入
- **ファイル**: `pom.xml`（親pom）
- **作業内容**:
  - JaCoCo Maven Plugin を追加
  - `mvn test` 実行時にカバレッジレポート生成
  - `target/site/jacoco/index.html` で閲覧可能に
- **参考設定**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

#### 1.2 order-service 統合テスト追加
- **対象**: `apps/services/order-service/src/test/java/`
- **作業内容**:
  - Saga実行フローのテスト（正常系/補償系）
  - `OrderController` の統合テスト
  - MockでSeata/Kafka依存を分離
- **目標カバレッジ**: 30%以上

#### 1.3 payment-service 統合テスト追加
- **対象**: `apps/services/payment-service/src/test/java/`
- **作業内容**:
  - PayPay API呼び出しのモックテスト
  - Webhook受信処理のテスト
  - 冪等性（重複Webhook）のテスト
- **目標カバレッジ**: 30%以上

#### 1.4 BFF 統合テスト追加
- **対象**: `apps/bff/src/test/java/`
- **作業内容**:
  - Firebase Auth検証のモックテスト
  - WebSocket通知のテスト
  - セッション管理のテスト
- **目標カバレッジ**: 25%以上

---

### 2. 静的解析導入

#### 2.1 Java静的解析（Checkstyle）
- **ファイル**: `pom.xml`（親pom）、`checkstyle.xml`（新規作成）
- **作業内容**:
  - Checkstyle Maven Plugin 追加
  - Google Java Style ベースの設定ファイル作成
  - `mvn checkstyle:check` で違反検出
- **参考設定**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <violationSeverity>warning</violationSeverity>
    </configuration>
</plugin>
```

#### 2.2 Java静的解析（SpotBugs）
- **ファイル**: `pom.xml`（親pom）
- **作業内容**:
  - SpotBugs Maven Plugin 追加
  - バグパターン検出を有効化
- **参考設定**:
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
</plugin>
```

#### 2.3 TypeScript/Vue静的解析（ESLint + Prettier）
- **対象**: `apps/web/`
- **作業内容**:
  - `pnpm add -D eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin prettier eslint-config-prettier`
  - `.eslintrc.cjs` 作成（Vue 3 + TypeScript設定）
  - `.prettierrc` 作成
  - `package.json` に `lint` / `format` スクリプト追加
- **参考設定**:
```json
// package.json scripts
{
  "lint": "eslint src --ext .vue,.ts,.tsx --fix",
  "format": "prettier --write src"
}
```

---

### 3. CI品質ゲート

#### 3.1 GitHub Actions テストステップ追加
- **ファイル**: `.github/workflows/deploy.yml`
- **作業内容**:
  - デプロイ前に `mvn test` 実行
  - テスト失敗時はデプロイを停止
  - JaCoCoカバレッジをアーティファクトとして保存
- **追加ステップ例**:
```yaml
- name: Run tests
  run: mvn test -B

- name: Upload coverage report
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report
    path: '**/target/site/jacoco/'
```

#### 3.2 Lintステップ追加
- **ファイル**: `.github/workflows/deploy.yml`
- **作業内容**:
  - Java: `mvn checkstyle:check` 実行
  - TypeScript: `pnpm lint` 実行
  - 違反時はデプロイを停止

---

## 🟡 Medium Priority（品質向上）

### 4. ドキュメント整備

#### 4.1 design-standards.md 完成
- **ファイル**: `docs/guide/design-standards.md`
- **作業内容**:
  - 行33の「TODO: 今後追加予定」セクションを埋める
  - コーディング規約（Java/TypeScript）
  - テスト戦略（単体/統合/E2E）
  - CI/CDパイプライン設計
  - セキュリティガイドライン

#### 4.2 feature-B 提案削除または完成
- **対象**: `docs/proposals/feature-B/`
- **作業内容**:
  - オプションA: スタブなので削除
  - オプションB: 具体的な機能提案を記載

#### 4.3 READMEにデモ動画/GIF追加
- **ファイル**: `README.md`
- **作業内容**:
  - 注文→決済フローのスクリーンキャプチャ（GIF）
  - アーキテクチャ図の更新
  - セットアップ手順の簡略化

#### 4.4 障害シナリオrunbook作成
- **ファイル**: `docs/runbook/failure-scenarios.md`（新規作成）
- **作業内容**:
  - PayPayダウン時の動作
  - Kafkaダウン時の動作
  - Seataダウン時の動作
  - 復旧手順

#### 4.5 libs/ ディレクトリの整理
- **対象**: `libs/`
- **作業内容**:
  - オプションA: 空ディレクトリなので削除
  - オプションB: README.md で用途を説明（将来の共通ライブラリ用）

---

### 5. コード整理

#### 5.1 タイポ修正
- **対象**: `docs/proposals/feaature-travel/`
- **作業内容**:
  - フォルダ名を `feature-travel` に修正
  - `git mv docs/proposals/feaature-travel docs/proposals/feature-travel`

#### 5.2 TODO/FIXMEコメント整理
- **対象**: 全ソースファイル
- **作業内容**:
  - `grep -r "TODO\|FIXME" apps/` で一覧取得
  - 対応不要なものは削除
  - 対応必要なものはGitHub Issueに起票

#### 5.3 未使用コード削除
- **作業内容**:
  - 未使用のimport文削除
  - 未使用のprivateメソッド削除
  - IDEのInspectionまたは静的解析で検出

---

## 🟢 Low Priority（差別化要素）

### 6. Observability実装（ADR-105）

#### 6.1 Micrometer Tracing導入
- **対象**: 全サービスの `pom.xml` / `application.yml`
- **作業内容**:
  - `micrometer-tracing-bridge-otel` 依存追加
  - `opentelemetry-exporter-otlp` 依存追加
  - Trace ID自動伝播設定

#### 6.2 Jaeger + Prometheus + Grafana構築
- **対象**: `platform/docker/demo/docker-compose-demo-env.yml`
- **作業内容**:
  - Jaeger All-in-one コンテナ追加
  - Prometheus コンテナ追加
  - Grafana コンテナ追加
  - 各サービスからのメトリクス収集設定

#### 6.3 Grafanaダッシュボード作成
- **作業内容**:
  - サービス間レイテンシ可視化
  - エラー率可視化
  - Saga実行時間可視化
  - スクリーンショットをREADMEに追加

---

### 7. Resilience実装（ADR-106）

#### 7.1 Resilience4j導入
- **対象**: `order-service`, `payment-service`, `bff`
- **作業内容**:
  - `resilience4j-spring-boot3` 依存追加
  - Circuit Breaker設定
  - Retry設定
  - フォールバック実装

---

### 8. 負荷テスト

#### 8.1 k6/Gatling負荷テスト作成
- **ファイル**: `scripts/load-test/` （新規作成）
- **作業内容**:
  - 注文フローの負荷テストスクリプト
  - 100 RPS での性能測定
  - レポート出力

#### 8.2 負荷テスト結果のドキュメント化
- **ファイル**: `docs/performance/load-test-results.md`（新規作成）
- **作業内容**:
  - スループット/レイテンシのグラフ
  - ボトルネック分析
  - 改善提案

---

## チェックリスト（公開前最終確認）

```
[ ] テストカバレッジ 30%以上
[ ] Checkstyle違反ゼロ
[ ] ESLint違反ゼロ
[ ] CI/CDでテスト/Lint実行
[ ] TODO/FIXMEコメント整理済み
[ ] タイポ修正済み
[ ] スタブファイル削除済み
[ ] READMEにデモ動画あり
[ ] 全ADRのステータス更新
```

---

## 参考: 現在の状態

| メトリック | 現在値 | 目標値 |
|----------|-------|-------|
| テストファイル数 | 9 | 20+ |
| テスト行数 | 801 | 2000+ |
| テストカバレッジ | 不明 | 30%+ |
| Checkstyle違反 | 不明 | 0 |
| ESLint違反 | 不明 | 0 |
| TODO/FIXMEコメント | 161 | 20以下 |

---

## 備考

- 各タスクは独立して実行可能
- 🔴 High Priority は公開前に必須
- 🟡 Medium Priority は品質向上のため推奨
- 🟢 Low Priority は差別化要素として検討
