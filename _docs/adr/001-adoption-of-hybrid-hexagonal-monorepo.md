# ADR 001: 拡張性を重視した構造改革（Monorepo & Hybrid Hexagonal）

## ステータス
承認済み

## 背景
初期のフラットなディレクトリ構成では、サービス数が増加した際にパッケージ間の境界が曖昧になり、結合度が高まる懸念があった。また、AI開発ツールにおいて、ドメインロジックとインフラ実装が混在していると、誤った責務のコードが生成されるリスクがあった。

## 決定事項
以下の2点を主軸としたリファクタリングを実施する。

1. **Monorepo構成への移行**:
   `apps/`（実行体）、`libs/`（共通基盤）、`platform/`（インフラ）に分類し、サービス間の物理的な独立性を確保する。
2. **Hybrid Hexagonalの採用**:
   クリーンアーキテクチャの思想を取り入れつつ、実務的な速度を損なわないレイヤー構成（web, application, domain, gateway）を定義する。

「過剰な抽象化による開発スピードの低下」と「密結合によるメンテナンス性の低下」のトレードオフを検討した結果、この **Hybrid Hexagonal** をプロジェクトの標準とする。

## 根拠
- **ドメインの保護**: ビジネスロジックを外部ライブラリから切り離すことで、技術スタックの刷新（例：MyBatisからJPAへの移行など）を容易にする。
- **AIコンテキストの最適化**: フォルダ構造自体が設計ルールを語る構成にすることで、AIツールの提案精度を向上させる。
- **実務への即応性**: 理論に固執せず、階層を深くしすぎないことで、開発の機敏性を維持する。

## 影響
- **ポジティブ**: コードのテスト容易性が向上し、新しいマイクロサービスの追加が容易になる。
- **ネガティブ**: パッケージ名変更に伴う一括置換が必要となる。


## 補足ybrid Hexagonal 概要
本アーキテクチャは、ドメインロジックを技術的詳細（フレームワーク、DB、外部API）から分離することを目的としています。

### アーキテクチャの階層と責務
| 層 (Package) | 役割 | 依存ルール |
| :--- | :--- | :--- |
| **web** | 外部からの入口。API定義、バリデーション。 | -> application, domain |
| **application** | ユースケースの実装。ドメインを操作し、一連の業務を遂行。 | -> domain, gateway(Port) |
| **domain** | ビジネスロジックの核。POJOによる実装。 | 依存なし |
| **gateway** | DB操作、他サービス呼び出しの実装（Adapter）。 | -> domain |

## 2. 他アーキテクチャとの明確な区別

### なぜ「Hybrid」なのか？
1. **フラットなネーミング**: 
   標準的なヘキサゴナルでは `adapter` パッケージの下に多層構造を作りますが、本プロジェクトでは Spring Boot の親和性を重視し、`web`, `gateway` という直感的なパッケージ名を採用しています。
2. **Portの配置**: 
   リポジトリ等のインターフェースは `domain` または `application` 層に配置し、実装を `gateway` 層に分離します。これにより、ドメインを汚染することなく技術スタックの交換（例：MyBatisからJPAへ）を可能にします。

## 3. 今後AIツールに守らせるルール
- 新しい機能を追加する際、ビジネスロジックは必ず `domain` パッケージに記述し、`web` フォルダ内にロジックを混入させないこと。
- 外部サービス呼び出しは必ず `gateway` 層で抽象化し、FeignClient を `application` から直接呼び出すのではなく、インターフェースを介すること。

## 4. 参考リンク
本設計のベースとなっている思想については、以下の資料を参照してください。

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design Reference (Eric Evans)](https://domainlanguage.com/ddd/reference/)
- [Clean Architecture (Uncle Bob)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Best Practices for Microservices](https://spring.io/guides/tutorials/rest/)
