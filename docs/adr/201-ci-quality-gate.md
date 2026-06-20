# Title
CI/CDパイプラインに品質ゲートを導入する

## Status (Proposed / Accepted / Deprecated)
Proposed

## Context
- 現在のec-demoは `.github/workflows/deploy.yml` でデプロイ自動化が実装されているが、**テスト実行や静的解析による品質チェックがデプロイ前に行われていない**。
- テストが失敗していても、静的解析に違反があっても、デプロイが実行される可能性がある。
- マイクロサービス構成（7サービス + フロントエンド）では、一つのサービスの品質低下が他サービスに波及するリスクがある。
- Tech Leadポートフォリオとして、「品質を担保するプロセス」の存在は必須である。
- ADR-200で静的解析ツールを導入する前提で、それをCIパイプラインに組み込む必要がある。

## Decision

### 1. 品質ゲートの定義

以下のチェックをすべてパスしない限り、デプロイを実行しない。

| ゲート | 対象 | 失敗条件 |
|-------|-----|---------|
| **Unit Test** | Java全サービス | テスト失敗 or カバレッジ閾値未達 |
| **Checkstyle** | Java全サービス | 違反あり（error レベル） |
| **SpotBugs** | Java全サービス | バグパターン検出（Medium以上） |
| **ESLint** | apps/web | エラーあり |
| **TypeCheck** | apps/web | 型エラーあり |

### 2. GitHub Actions ワークフロー構成

```yaml
# .github/workflows/ci.yml（新規作成）
name: CI Quality Gate

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  # ============================================
  # Java Backend Quality Gate
  # ============================================
  java-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests with coverage
        run: mvn test -B

      - name: Checkstyle
        run: mvn checkstyle:check -B

      - name: SpotBugs
        run: mvn spotbugs:check -B

      - name: Upload coverage report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: '**/target/site/jacoco/'
          retention-days: 7

  # ============================================
  # Frontend Quality Gate
  # ============================================
  frontend-quality:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: apps/web
    steps:
      - uses: actions/checkout@v4

      - name: Setup pnpm
        uses: pnpm/action-setup@v3
        with:
          version: 9

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'pnpm'
          cache-dependency-path: apps/web/pnpm-lock.yaml

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: TypeScript type check
        run: pnpm vue-tsc --noEmit

      - name: ESLint
        run: pnpm lint

      - name: Build
        run: pnpm build
```

### 3. デプロイワークフローとの連携

```yaml
# .github/workflows/deploy.yml（既存を修正）
name: Deploy

on:
  push:
    branches: [develop]
  workflow_dispatch:

jobs:
  # 品質ゲートを先に実行
  quality-gate:
    uses: ./.github/workflows/ci.yml

  # 品質ゲート通過後にデプロイ
  deploy:
    needs: [quality-gate]
    runs-on: ubuntu-latest
    # ... 既存のデプロイステップ
```

### 4. カバレッジ閾値の設定

JaCoCoでカバレッジ閾値を設定し、未達の場合はビルド失敗とする。

```xml
<!-- pom.xml (親pom) -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.30</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 5. PR時のステータスチェック設定

GitHub Branch Protection Ruleで以下を設定する:

| 設定項目 | 値 |
|---------|---|
| Required status checks | `java-quality`, `frontend-quality` |
| Require branches to be up to date | ✅ |
| Require conversation resolution | ✅ |

### 6. 段階的導入

| Phase | 内容 | 失敗時の動作 |
|-------|-----|-------------|
| Phase 1 | CI実行、警告表示のみ | デプロイ続行 |
| Phase 2 | Checkstyle/ESLint違反でビルド失敗 | デプロイ停止 |
| Phase 3 | SpotBugs違反でビルド失敗 | デプロイ停止 |
| Phase 4 | カバレッジ閾値チェック | デプロイ停止 |

## Alternatives

### A. GitHub Actions以外のCI（CircleCI, Jenkins, GitLab CI）
- **不採用理由**: 既にGitHub Actionsでデプロイが構築されており、統一性を優先。追加のCI基盤は運用コストが増加する。

### B. ローカルのみでの品質チェック（pre-commit hook）
- Huskyでコミット前にlint/testを実行する方式。
- **不採用理由**: ローカル環境の差異、hookのスキップ（`--no-verify`）のリスク。CIでの強制チェックと併用は検討可能。

### C. 品質ゲートなし（現状維持）
- **不採用理由**: 品質低下したコードがデプロイされるリスク。ポートフォリオとして「品質プロセスがない」という印象を与える。

### D. 段階的導入なし（即座に全ゲートを強制）
- **不採用理由**: 既存コードの違反が多数ある場合、修正完了までデプロイがブロックされる。段階的導入で移行期間を設ける。

## Consequences

### メリット
- **品質の強制**: テスト失敗や静的解析違反のコードがデプロイされない。
- **早期発見**: PR段階で品質問題を検出し、本番障害を予防。
- **自動化**: 人手によるレビューの負荷を軽減。
- **透明性**: GitHub上でチェック結果が可視化され、チーム全員が品質状況を把握可能。
- **ポートフォリオ価値**: 「CI/CDで品質を担保している」ことを証明できる。

### デメリット・注意点
- **CI実行時間**: テスト + 静的解析で5〜10分程度のオーバーヘッドが発生。
- **初期対応コスト**: 既存コードの違反修正が必要。
- **Flaky Test**: 不安定なテストがあるとCIが頻繁に失敗し、開発者体験が悪化する。テストの安定化が前提。
- **GitHub Actions課金**: Private Repoの場合、実行時間に応じた課金が発生（Public Repoは無料）。

### 成功指標

| 指標 | 目標値 |
|-----|-------|
| CI成功率 | 90%以上 |
| PRマージまでの平均時間 | 24時間以内 |
| 本番障害（品質起因） | 0件/月 |
| カバレッジ | 30%以上 |

## References

### 公式ドキュメント
- [GitHub Actions](https://docs.github.com/en/actions) — CI/CD
- [GitHub Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches) — Required status checks
- [JaCoCo Maven Plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html) — カバレッジ

### 関連ADR
- ADR-200: 静的解析ツール導入（本ADRの前提）
- ADR-105: Observability基盤（CI/CDメトリクスの可視化と連携可能）
