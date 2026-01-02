# `_docs`（ドキュメント正本 / Single Source of Truth）

このリポジトリにおけるドキュメントの正本（single source of truth）は、**main 側の `_docs/`** に集約します。  
git worktree で複数ブランチ（例：`feature/A`, `feature/B`）を並行開発しても、AI agent を含む複数作業者がドキュメントで衝突しにくい運用を目的としています。

## 編集ルール（重要）

### main 側のみ編集してよい領域（正本）
以下は **ドキュメント正本** です。**feature ブランチからの直接編集は禁止**します。

- `_docs/architecture/`（全体設計・構成）
- `_docs/adr/`（設計判断の記録）
- `_docs/api/`（API 仕様）
- `_docs/runbook/`（運用・手順）

### feature ブランチで編集可能な領域（提案・作業ログのみ）
feature ブランチで編集できるのは、次の 2 箇所 **のみ** とします。

- `_docs/proposals/<feature-name>/`
- `_docs/worklog/`

## 運用フロー（提案 → レビュー → main 反映）
設計変更・仕様変更が必要な場合は、次の手順で進めます。

1. feature ブランチで `_docs/proposals/<feature-name>/` に提案（背景・差分・影響）を書く
2. レビューで合意を取る
3. 合意後、main 側の正本（`_docs/architecture/` / `adr/` / `api/` / `runbook/`）へ反映する

## ADR（Architecture Decision Record）運用
設計判断は口頭やチャットに埋もれないよう、**必ず ADR として `_docs/adr/` に残します**。

- 新規の判断：`_docs/adr/` に連番で追加（例：`001-some-decision.md`）
- テンプレート：`_docs/adr/000-template.md`
- ステータス：`Proposed` → `Accepted`（必要に応じて `Deprecated`）

## worklog の目的
`_docs/worklog/` は、並行作業時の状況共有と引き継ぎを円滑にするためのログ置き場です。

- 作業メモ（今日やったこと）
- 未解決課題（詰まり・判断待ち・不明点）
- 次の TODO（次回着手点、依存、確認事項）

## クイックルールサマリ
迷ったら `docs-policy.md` も参照してください（編集禁止事項と運用ルールの要点を短くまとめています）：`_docs/docs-policy.md`
