# Role
あなたはシニアバックエンドエンジニア / テックリードです。
ECサイトの注文・決済システムを、実務レベルの設計思想に基づきリファクタリングしてください。

# 対象リポジトリ
https://github.com/jasonw-lab/ec-demo
Codeは本プロンプトのソース

# 背景・目的
本リポジトリは転職用ポートフォリオとして、
Amazon / メルカリに近い「EC 注文・決済のあるべき姿」を示すことが目的です。

- マイクロサービス構成
- 外部決済（PayPay）の非同期性を正しく扱う
- Seata Saga を **long business の一部（初期整合性）**として利用
- long transaction を避ける

# 採用する設計思想（重要）
- EC は最終的整合性（Eventually Consistent）を前提とする
- 注文〜決済完了は long business
- トランザクションは常に短く保つ
- 外部決済の完了は Saga に含めない

# サービス構成（固定）
- BFF
- Order Service（業務の中心）
- Inventory Service
- Payment Service（PayPay API 専用）
- PayPay のみ対応で OK

# Order の責務（最重要）
Order Service は long business の「唯一の真実」とする。

Order 状態は以下を基本とする：
- CREATED
- PAYMENT_PENDING
- PAID
- CANCELLED

Saga 成功 = 注文成功ではない  
Saga 成功 = 「決済待ち状態に入った」

# Inventory の責務
- 物理リソースの整合性管理
- 状態：
  - AVAILABLE
  - RESERVED
  - COMMITTED
  - RELEASED

# Payment の責務
- PayPay API との境界
- 非同期決済の吸収
- Webhook 冪等制御

Payment 状態：
- INIT
- PENDING
- SUCCESS
- FAILED

# Seata Saga の使い方（厳守）
## Saga の責務
- 注文初期化フェーズの安全な実行

## Saga に含める処理
- InitOrder（注文作成 → PAYMENT_PENDING）
- ReserveStock（在庫仮確保）
- RequestPayment（PayPay 決済要求）

## Saga に含めない処理
- 決済完了待ち
- PayPay Webhook
- 返金処理

Saga 名は以下の意味が分かるものに変更する：
- order_create_saga ❌
- order_initialization_saga ⭕️

# 決済成功・失敗時の処理
## Saga 内で失敗した場合（即時）
- Seata Saga による補償
  - 在庫解放
  - 注文キャンセル

## Saga 完了後の決済結果（Webhook）
- Seata は関与しない
- Payment Service が Webhook を受信
- Order Service が状態遷移を判断
  - SUCCESS → Order=PAID, Inventory=COMMITTED
  - FAILED  → Order=CANCELLED, Inventory=RELEASED

# 実装方針
- @GlobalTransactional は使用しないか、極小範囲に限定
- Saga 補償メソッドは必ず冪等にする
- 補償処理で外部 API を呼ばない
- Order 状態を必ずガード条件として使用する

# リファクタリング内容（お願い）
1. 現在のコード構造を分析
2. 上記設計に沿って以下を実施
   - 責務が混ざっている箇所の分離
   - Saga 定義の修正
   - Order / Inventory / Payment の状態遷移整理
3. 必要に応じてクラス・メソッド名を修正
4. README に設計思想が伝わる説明を追記

# 注意事項
- フレームワーク仕様に引きずられず、業務モデルを優先すること
- 実装を増やしすぎず「思想が伝わる」ことを重視
- 既存機能は極力壊さない（最小改修）

# ゴール
- 「なぜこの設計なのか」を説明できる EC システム
- 面接で Seata / Saga / 決済設計について深掘りされても耐えられる構成
