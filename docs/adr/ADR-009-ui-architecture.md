# Title
UIアーキテクチャ、フォルダ構成、OSSテンプレート利用方針、状態管理方針を定義する

## Status (Proposed / Accepted / Deprecated)
Proposed

## Context

- ec-demoは転職・技術ポートフォリオ用途も兼ねるため、単に画面を作るだけではなく、**実務で説明できるUIアーキテクチャ**として整理する必要がある。
- 今後、不動産検索UIのような検索・一覧・詳細・管理画面を追加する可能性があり、画面数や機能が増えても破綻しにくい構成が必要である。
- AI UI生成ツール（v0等）を活用すると、React + Tailwind CSS + shadcn/ui ベースの画面を短時間で生成できる。一方で、生成コードをそのまま肥大化させると、状態管理・API接続・コンポーネント分割が崩れやすい。
- 既存OSSテンプレートを利用すると初期構築は速いが、不要機能・過剰な抽象化・ライセンス確認・構成理解コストが発生する。
- UI状態、サーバーデータ、認証状態、検索条件、フォーム状態など、状態の種類ごとに責務を分けないと、グローバルストアが肥大化しやすい。

## Decision

### 1. UI技術スタック

フロントエンドは以下を第一候補とする。

```text
React + TypeScript + Vite + Tailwind CSS + shadcn/ui
```

#### 採用理由

| 技術 | 採用理由 |
|---|---|
| React | v0などAI UI生成ツールとの相性が高く、ポートフォリオとして見栄えの良いUIを作りやすい |
| TypeScript | API DTO、フォーム、状態管理の型安全性を確保できる |
| Vite | 起動が速く、React/TypeScript構成がシンプル |
| Tailwind CSS | 画面単位のデザイン調整が速く、AI生成コードとの親和性が高い |
| shadcn/ui | Button/Card/Dialog/Table/Form等をコードとして取り込めるため、見た目と実装を調整しやすい |

### 2. UIアーキテクチャ方針

画面規模の増加を見越して、**feature-based architecture** を採用する。

```text
src/
  app/
    router/
    providers/
    layouts/
  pages/
    home/
    property-search/
    property-detail/
    favorites/
    inquiry/
    admin/
  features/
    property-search/
    property-detail/
    favorite/
    inquiry/
    auth/
    admin-property/
  entities/
    property/
    station/
    user/
    inquiry/
  shared/
    api/
    components/
    hooks/
    lib/
    types/
    utils/
    constants/
```

#### 各ディレクトリの責務

| ディレクトリ | 責務 |
|---|---|
| `app/` | ルーティング、Provider、全体レイアウト、アプリ初期化 |
| `pages/` | URLに対応するページコンポーネント。業務ロジックは持ちすぎない |
| `features/` | 検索、問い合わせ、お気に入り等のユースケース単位のUI・hooks・logic |
| `entities/` | Property、User、Station等の業務エンティティの型・表示部品・変換処理 |
| `shared/` | 汎用UI、APIクライアント、共通hooks、共通util、定数 |

### 3. 不動産検索UI向けの想定構成

```text
src/
  pages/
    property-search/
      PropertySearchPage.tsx
    property-detail/
      PropertyDetailPage.tsx

  features/
    property-search/
      components/
        SearchConditionPanel.tsx
        PropertyResultList.tsx
        PropertyMapPanel.tsx
        SortSelect.tsx
      hooks/
        usePropertySearchParams.ts
      model/
        searchSchema.ts

    favorite/
      components/
        FavoriteButton.tsx
      hooks/
        useToggleFavorite.ts

  entities/
    property/
      api/
        propertyApi.ts
      model/
        propertyTypes.ts
      components/
        PropertyCard.tsx
        PropertyImageGallery.tsx
        PropertySummary.tsx
```

#### 設計ルール

- `pages/` は画面の組み立てに集中し、詳細な業務処理は `features/` に寄せる。
- `features/` はユースケース単位で閉じる。例：物件検索、問い合わせ、お気に入り。
- `entities/` は業務エンティティ単位で再利用される型・API・表示部品を置く。
- `shared/` には業務知識を入れすぎない。汎用部品に限定する。
- v0で生成した大きな画面コードは、そのまま置かず、`pages/features/entities/shared` に分割して取り込む。

### 4. OSSテンプレート利用方針

#### 結論

**既存OSSテンプレートを丸ごと採用しない。**

ただし、以下のように「参考・部分利用」は許可する。

| 対象 | 方針 |
|---|---|
| shadcn/ui | 公式CLIで必要コンポーネントのみ追加する |
| v0生成コード | UI初期案として利用し、プロジェクト構成に合わせて分割する |
| Tailwind設定 | 最小構成から開始し、必要に応じてテーマ拡張する |
| 管理画面テンプレート | レイアウト・ナビゲーションの参考に留め、丸ごと導入しない |
| SaaS starter template | 認証・課金・DB等の過剰機能が多いため採用しない |

#### OSSテンプレートを丸ごと採用しない理由

- 不要な認証、課金、チーム管理、メール送信等が入りやすく、学習・保守コストが増える。
- ポートフォリオとして説明する際に「自分で設計した構成」ではなく「テンプレート流用」に見えやすい。
- ライセンス、依存関係、アップデート方針の確認が必要になる。
- 既存バックエンド（Spring Boot / Java）とのAPI設計に合わせて調整しにくい場合がある。
- UI生成ツールで得たコードを整理する練習・設計説明の機会が減る。

### 5. 状態管理方針

状態を種類ごとに分離する。

| 状態の種類 | 管理方法 | 例 |
|---|---|---|
| サーバーデータ | TanStack Query | 物件一覧、物件詳細、お気に入り一覧、問い合わせ一覧 |
| URLに残す検索条件 | React Router / URLSearchParams | エリア、家賃、間取り、駅徒歩、ページ番号、ソート条件 |
| ローカルUI状態 | useState / useReducer | Dialog開閉、Sheet開閉、タブ選択、画像ギャラリーの表示状態 |
| フォーム状態 | React Hook Form + Zod | 問い合わせフォーム、管理画面の物件登録フォーム |
| グローバルUI状態 | Zustand | サイドバー開閉、ログインユーザー概要、軽量なUI設定 |
| 認証状態 | HttpOnly Cookie + `/me` API + TanStack Query | 管理者ログイン状態、ユーザー情報 |

#### 原則

- サーバーデータをZustand等のグローバルストアに重複保持しない。
- 物件検索条件はURLに載せる。再読み込み・共有・ブラウザバックに対応するため。
- 検索結果はTanStack Queryで取得し、キャッシュ・再取得・ローディング・エラー状態を管理する。
- フォームはReact Hook Form + Zodで入力値とバリデーションを管理する。
- Zustandは最小限に限定し、何でも入れる「巨大store」にしない。

### 6. API接続方針

APIクライアントは `shared/api` に共通実装を置き、各エンティティまたはfeature側でAPI関数を定義する。

```text
src/
  shared/
    api/
      httpClient.ts
      apiError.ts

  entities/
    property/
      api/
        propertyApi.ts

  features/
    favorite/
      api/
        favoriteApi.ts
```

#### API層のルール

- `httpClient.ts` でBase URL、共通ヘッダー、エラー変換を統一する。
- APIレスポンス型は `entities/*/model` に定義する。
- TanStack Queryのquery keyはfeature/entityごとに定義し、文字列ベタ書きを避ける。
- Backend DTOとFrontend型の差分が大きい場合は、UI向けmapperを用意する。

### 7. コンポーネント設計方針

```text
shared/components/  汎用UI
entities/*/components/  業務エンティティ表示部品
features/*/components/  ユースケース部品
pages/*/  ページ組み立て
```

#### 判断基準

| 部品 | 配置先 |
|---|---|
| Button、Dialog、Card、Input | `shared/components` または shadcn/ui 標準配置 |
| PropertyCard | `entities/property/components` |
| SearchConditionPanel | `features/property-search/components` |
| FavoriteButton | `features/favorite/components` |
| PropertySearchPage | `pages/property-search` |

### 8. 導入ステップ

#### Step 1: 最小構成

```text
React + TypeScript + Vite
Tailwind CSS
shadcn/ui
React Router
TanStack Query
```

#### Step 2: 不動産検索UIの基本画面

- トップ検索画面
- 物件一覧画面
- 物件詳細画面
- お気に入り画面
- 問い合わせ画面

#### Step 3: 状態管理導入

- URLSearchParamsで検索条件を管理
- TanStack Queryで物件検索APIを接続
- React Hook Form + Zodで問い合わせフォームを実装
- Zustandは必要になった時点で最小限導入

#### Step 4: 管理画面

- 管理者ログイン
- 物件一覧管理
- 物件登録・編集
- 画像アップロード
- 問い合わせ管理

#### Step 5: AI生成コード整理

- v0生成コードを画面単位からfeature/entity単位に分割
- ダミーデータをAPI接続に置換
- 表示部品と業務ロジックを分離
- READMEにUI設計方針と画面キャプチャを追加

## Alternatives

### A. Vue 3 + TypeScript + Element Plus

#### メリット

- ユーザーの既存経験（Vue.js）と親和性が高い。
- 管理画面や業務システムUIを作りやすい。
- Element Plusのコンポーネントが豊富で実装が速い。

#### 不採用理由

- 今回の不動産検索UIでは、ポートフォリオとして見た目のモダンさとAI生成効率を重視する。
- v0 + shadcn/ui の生成コード活用を前提にするとReact構成の方が効率が良い。

### B. React + Ant Design

#### メリット

- 管理画面との相性が高い。
- Table、Form、Modal等の業務コンポーネントが充実している。

#### 不採用理由

- 見た目が管理画面寄りになり、不動産検索サイトの一般ユーザー向けUIとしては硬く見えやすい。
- Tailwind/shadcn/uiほど細かいデザイン調整やAI生成コードとの相性が高くない。

### C. Next.jsフルスタック構成

#### メリット

- App Router、SSR/SSG、SEO対応に強い。
- Vercelデプロイとの相性が高い。

#### 不採用理由

- ec-demoのバックエンドはJava/Spring Bootを主役にしたい。
- フロントエンド側までフルスタック化すると、API責務が分散し、Spring Bootバックエンドのアピールが弱くなる。
- まずはSPA + Spring Boot APIの構成で十分。

### D. OSS SaaS Starter Templateを丸ごと採用

#### メリット

- 初期画面、認証、レイアウト、設定が揃っており、立ち上げは速い。

#### 不採用理由

- 不要機能が多く、学習・保守コストが増える。
- ポートフォリオとして「設計した」より「テンプレートを使った」印象になりやすい。
- Spring Boot APIとの統合方針を自分で設計しにくい。

### E. Redux Toolkit中心の状態管理

#### メリット

- 大規模アプリで実績があり、状態遷移が明示的。

#### 不採用理由

- 今回のUIではサーバーデータはTanStack Query、検索条件はURL、フォームはReact Hook Formで十分に分離できる。
- Reduxに集約するとボイラープレートが増え、DEMO開発速度が落ちる。

## Consequences

### メリット

- v0やAI生成ツールを活用しやすく、短期間で見栄えの良いUIを作成できる。
- feature-based構成により、検索・詳細・お気に入り・問い合わせ・管理画面を追加しても整理しやすい。
- 状態管理を種類別に分離することで、グローバルストア肥大化を防げる。
- URLに検索条件を保持するため、検索結果の共有、ブラウザバック、再読み込みに強い。
- TanStack Queryにより、API取得、キャッシュ、ローディング、エラー、再取得を標準化できる。
- OSSテンプレート丸ごと採用を避けることで、設計意図を面接・READMEで説明しやすい。

### デメリット・注意点

- React + Tailwind + shadcn/ui の設計ルールを明確にしないと、コンポーネント分割が乱れやすい。
- v0生成コードは品質が一定ではないため、命名、型、責務分割、アクセシビリティを必ず見直す必要がある。
- Vue経験を強く見せたい場合は、React採用理由を明確に説明する必要がある。
- shadcn/uiはコンポーネントコードをプロジェクト内に持つため、アップデート時は差分管理が必要になる。
- 状態管理ライブラリを複数使うため、何をどこで管理するかのルールを守る必要がある。

## References

- React: https://react.dev/
- Vite: https://vite.dev/
- Tailwind CSS: https://tailwindcss.com/
- shadcn/ui: https://ui.shadcn.com/
- TanStack Query: https://tanstack.com/query/latest
- React Hook Form: https://react-hook-form.com/
- Zod: https://zod.dev/
- Zustand: https://zustand-demo.pmnd.rs/
- v0 by Vercel: https://v0.dev/
