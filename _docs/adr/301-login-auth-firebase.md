# Title
ログイン認証にFirebase Authを採用する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- フロントはWebであり、メール/パスワードやソーシャルログインを素早く提供したい。
- 認証の秘匿情報を自前で保持せず、運用負荷を下げたい。
- バックエンドはBFFを入口として、内部ユーザーIDにマッピングする必要がある。

## Decision
- フロントはFirebase Auth（Client SDK）でサインインし、ID Tokenを取得する。
- BFFはFirebase Admin SDKでID Tokenを検証し、Redisにセッションを発行する。
- account-serviceでFirebase UIDと内部ユーザーIDを同期し、各サービスは内部IDで処理する。

## Alternatives
- 自前認証（パスワード管理・MFA・脆弱性対応の運用負荷が高い）。
- 外部IdP（Auth0/Cognito等）（導入コストと学習コストが増える）。
- BFFでJWTを直接発行（鍵管理とローテーションを自前で行う必要がある）。

## Consequences
- Firebaseの可用性や設定に依存するため、サービスアカウント管理が必須になる。
- BFFでセッションを管理することで、バックエンドは認可・本人情報更新を統一できる。
- セッションとID Tokenの有効期限管理が重要になる。

## References
- bff/src/main/java/com/demo/ec/controller/AuthController.java
- bff/src/main/java/com/demo/ec/config/FirebaseConfig.java
- bff/src/main/java/com/demo/ec/auth/SessionService.java
- _docs/docker/demo/sql/mysql-init/10-business-ddl.sql
