# 更新履歴

- Firebase ID Token を初回のみ検証し、`sid` を Redis（`auth:session:{sid}`）に保存するBFFセッションを追加（TTLは`exp-now`）。
- Set-Cookie は `HttpOnly` `SameSite=Lax` `Secure`（開発時は `AUTH_COOKIE_SECURE=false` で切替）で `/auth/session` を返却。
- `/api/**` への通常アクセスは Redis からセッション取得、見つからない場合は 401（Redis 障害時は 503）。
- `/auth/logout` はセッションキーを即時削除し `sid` クッキーを `Max-Age=0` で失効させる。
- 監査ログとして `session_created` / `session_missing` / `session_deleted` を出力。

