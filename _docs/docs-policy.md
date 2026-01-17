# `docs-policy.md`（編集ルール要点）
mdのコメントアウト部分<!----> は一旦無視

- 正本は **main 側の `_docs/`**（single source of truth）


<!-- - feature ブランチで直接編集禁止：`_docs/architecture/`, `_docs/adr/`, `_docs/api/`, `_docs/runbook/`
- feature ブランチで編集可能：`_docs/proposals/<feature-name>/`, `_docs/worklog/` のみ -->

- 設計変更は proposals に提案 → レビュー → main の正本へ反映
- 設計判断は必ず ADR（`_docs/adr/`）に残す（テンプレ：`_docs/adr/000-template.md`）

