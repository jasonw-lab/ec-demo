# Title
静的解析ツールを導入しコード品質を標準化する

## Status (Proposed / Accepted / Deprecated)
Proposed

## Context
- 現在のec-demoは7サービス（BFF + 6バックエンド）+ Vueフロントエンドで構成されるマイクロサービスであり、複数の開発者（人間/AI）が同時に作業する前提である。
- コーディングスタイルや品質基準が明文化されておらず、レビューの属人化やスタイルの不統一が発生し得る。
- Java 200ファイル / TypeScript 22ファイルの規模があり、手動レビューですべての品質問題を検出することは困難である。
- Tech Leadポートフォリオとして公開する際、静的解析未導入は「品質基準が曖昧」という印象を与えるリスクがある。
- AI駆動開発においても、静的解析の結果をフィードバックとして活用することで、生成コードの品質向上が期待できる。

## Decision

### 1. Java静的解析: Checkstyle + SpotBugs

#### Checkstyle（コーディングスタイル）
- Google Java Style Guideをベースにしたルールセットを採用する。
- プロジェクト固有のカスタマイズ（行長120文字、特定パターンの許容等）は `checkstyle.xml` で管理する。

```xml
<!-- pom.xml (親pom) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <violationSeverity>warning</violationSeverity>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.14.0</version>
        </dependency>
    </dependencies>
</plugin>
```

#### SpotBugs（バグパターン検出）
- NullPointerException、リソースリーク、並行処理バグ等のパターンを検出する。
- Find Security Bugsプラグインを併用し、セキュリティ脆弱性も検出する。

```xml
<!-- pom.xml (親pom) -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <failOnError>true</failOnError>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.13.0</version>
            </plugin>
        </plugins>
    </configuration>
</plugin>
```

### 2. TypeScript/Vue静的解析: ESLint + Prettier

#### ESLint（静的解析）
- `@typescript-eslint` パーサーでTypeScript対応。
- `eslint-plugin-vue` でVue 3 SFC対応。
- `vue/vue3-recommended` ルールセットをベースに採用。

```javascript
// apps/web/.eslintrc.cjs
module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-recommended',
    'prettier'
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }]
  }
};
```

#### Prettier（フォーマッタ）
- コードフォーマットをPrettierに一元化し、ESLintとの競合を `eslint-config-prettier` で解消する。

```json
// apps/web/.prettierrc
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "vueIndentScriptAndStyle": true
}
```

### 3. 実行コマンド

| 対象 | コマンド | 説明 |
|-----|---------|------|
| Java全体 | `mvn checkstyle:check` | Checkstyleチェック |
| Java全体 | `mvn spotbugs:check` | SpotBugsチェック |
| フロント | `pnpm lint` | ESLintチェック |
| フロント | `pnpm format` | Prettierフォーマット |
| フロント | `pnpm lint:fix` | ESLint自動修正 |

### 4. 設定ファイル配置

```
ec-demo/
├── checkstyle.xml           # Java Checkstyle設定
├── spotbugs-exclude.xml     # SpotBugs除外設定（必要に応じて）
├── pom.xml                  # 親pomにプラグイン定義
└── apps/web/
    ├── .eslintrc.cjs        # ESLint設定
    ├── .prettierrc          # Prettier設定
    └── package.json         # lint/formatスクリプト
```

## Alternatives

### A. SonarQube / SonarCloud
- 包括的なコード品質ダッシュボードを提供。カバレッジ、重複コード、技術的負債を可視化。
- **不採用理由**: SonarQubeサーバーの運用が必要。SonarCloudは無料枠あるが、シンプルな構成を優先し、将来的な導入として検討。

### B. PMD（Java）
- Checkstyleと類似のルールベース解析。
- **不採用理由**: CheckstyleとSpotBugsの組み合わせで主要な品質問題をカバー可能。ツール数を最小化する。

### C. Biome（TypeScript/Vue）
- ESLint + Prettierの代替となる高速ツール。
- **不採用理由**: Vue SFCサポートが限定的。エコシステムの成熟度でESLint + Prettierが優位。

### D. 静的解析未導入（現状維持）
- 導入コストなし。
- **不採用理由**: 品質基準が曖昧になり、レビューの属人化が進む。ポートフォリオとしての信頼性が低下する。

## Consequences

### メリット
- **品質の標準化**: コーディングスタイルが統一され、レビューコストが低減する。
- **早期バグ検出**: SpotBugsでNullPointerException、リソースリーク等を開発時に検出。
- **セキュリティ強化**: Find Security Bugsでインジェクション、暗号化の誤用等を検出。
- **AI生成コードの品質向上**: 静的解析結果をAIへのフィードバックとして活用可能。
- **ポートフォリオ価値**: 品質管理プロセスの存在を証明できる。

### デメリット・注意点
- **初期対応コスト**: 既存コードの違反修正が必要（段階的に対応可能）。
- **ビルド時間増加**: 静的解析実行により数秒〜数十秒のオーバーヘッドが発生。
- **False Positive対応**: 一部のルールで誤検知が発生する場合があり、除外設定の調整が必要。
- **学習コスト**: 開発者がルールを理解し、遵守する必要がある。

### 段階的導入計画

| Phase | 対象 | 内容 |
|-------|-----|------|
| Phase 1 | 全サービス | Checkstyle導入、警告レベルでCI実行 |
| Phase 2 | 全サービス | SpotBugs導入、主要バグパターン検出 |
| Phase 3 | apps/web | ESLint + Prettier導入 |
| Phase 4 | CI/CD | 違反時にビルド失敗に昇格 |

## References

### 公式ドキュメント
- [Checkstyle](https://checkstyle.org/) — Javaコーディングスタイルチェッカー
- [SpotBugs](https://spotbugs.github.io/) — Javaバグパターン検出
- [Find Security Bugs](https://find-sec-bugs.github.io/) — セキュリティ脆弱性検出
- [ESLint](https://eslint.org/) — JavaScript/TypeScript静的解析
- [Prettier](https://prettier.io/) — コードフォーマッタ
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
