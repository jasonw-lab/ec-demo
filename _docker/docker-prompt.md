あなたは **シニアバックエンドエンジニア（Docker・ミドルウェア構成・デプロイに非常に詳しい）** として回答してください。  
細かいコード修正内容ではなく、**構成方針・ファイル構成・設定ポイント** を中心に提案してください。

---

## 背景・目的

- 既存 VPS 上に **smart-retail** の Docker 構成が動作している。
- 新しく **ec-demo（feature/transacion ブランチ）** を、  
  **既存環境を極力壊さず・最小限の変更だけで追加して demo を動かしたい。**

やりたいことは：

- smart-retail（既存）の Docker 環境に、ec-demo 用コンテナ群を追加
- 既存ネットワーク／nginx を活かして、ブラウザから ec-demo を確認できるようにする
- 細かいコード修正よりも、「どういう compose / nginx / ネットワーク設計にするか」の指針が欲しい


mart-retailは既存のリポジトリのこと
---

## 参照リポジトリ

- 既存環境（smart-retail / VPS 側本番構成）
ec-demo/_docker/existing

- 追加したい demo（ec-demo / トランザクションデモ）
  ec-demo/_docker/demo

---

## やってほしいこと

1. **サービス構成の整理**
   - ec-demo 側で起動すべきサービス（例：account-service, order-service, storage-service, BFF, front など）を列挙し、
   - smart-retail と共存させる前提で、論理構成図レベルで整理してください。

2. **docker-compose 設計方針**
   - 既存の docker` 側の compose は **最小限の修正のみ** にしたいです。
   - 可能であれば：
     - smart-retail は既存 compose をほぼそのまま
     - ec-demo 用に **別の `docker-compose.ec-demo.yml`** を作成して追加起動  
       という構成にしてください。
   - どのサービスを ec-demo 側 compose に含めるか、  
     どのネットワークを `external` で再利用するか（例：`net2`）を設計してください。

3. **ミドルウェア構成の方針**
   - demo 用として  
     - MySQL（ec-demo 専用DB）  
     - Seata（ec-demo 専用インスタンス）  
     を新規コンテナとして用意する方針で構いません。
   - smart-retail 側の MySQL / Seata とはポート衝突しないようにしてください。
   - どのような命名・ポート割り当てにすると管理しやすいか、ポリシーを言語化してください。
   - 既存 Redis / RabbitMQ / Nacos などを再利用すべきか／敢えて使わない方が安全か、判断方針をください。

4. **ネットワーク構成**
   -　既存一番修正が少なめ
     提案してください。

5. **nginx 設計（リバースプロキシ）**
   - nginx から ec-demo フロント / BFF へルーティングする構成を設計してください。
   - 例：
     - `http://ec-demo.local/` → ec-demo front コンテナ
     - `http://ec-demo.local/ec-api/` → ec-demo BFF コンテナ
   - 既存 smart-retail 用の nginx を最小限修正

6. **ポート衝突・命名ポリシー**
   - smart-retail と ec-demo が同時稼働してもポート衝突しないよう、
     - MySQL / Seata / BFF / front などのホストポート案を示してください。
   - 「本番アクセスは nginx 経由にするが、デバッグ用にホスト公開ポートも残す」ような現実的な案があれば教えてください。

7. **VPS への追加デプロイ手順（高レベル）**
   - 既存 smart-retail がすでに動いている前提で、
     - ec-demo 側の compose ファイル配置
     - build / up -d
     - nginx 設定追加 → reload
   - という一連の手順を、**具体的なコマンド例付きでフローとして示してください。
   - ただしコードや Dockerfile 個別修正の詳細ではなく、  
     「運用手順書」のようなレベルの説明にしてください。

---



---
