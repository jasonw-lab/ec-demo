# MinIO 環境設定（ec-demo / Elasticsearch 連携）

## 1. MinIO 起動（docker-compose）

`_docs/proposals/feature-elasticsearch/docker-compose-demo-elasticsearch.yml` を利用して起動する。

```bash
docker-compose -f _docs/proposals/feature-elasticsearch/docker-compose-demo-elasticsearch.yml up -d ec-demo-minio
```

## 2. 必要な環境変数（es-service）

MinIO 側の接続情報と公開URLを設定する。

```
MINIO_ENDPOINT=http://<minio-host>:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET=ec-demo
MINIO_PUBLIC_BASE_URL=http://<minio-host>:9000/ec-demo
```

ポイント:
- `MINIO_PUBLIC_BASE_URL` は **ブラウザから直接アクセスできるURL** を指定する。
- 末尾にバケット名 `/ec-demo` を付ける。

ローカル例:
```
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_BASE_URL=http://localhost:9000/ec-demo
```

Docker 上で別ホストにある場合:
```
MINIO_ENDPOINT=http://192.168.1.199:9000
MINIO_PUBLIC_BASE_URL=http://192.168.1.199:9000/ec-demo
```

## 3. バケット作成（mc）

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin123
mc mb local/ec-demo
```

## 4. 公開アクセス（Anonymous Read）

画像をブラウザで表示するために、バケットを公開する。

```bash
mc anonymous set download local/ec-demo
```

MinIO コンソールで設定する場合:
- `http://localhost:9001` にログイン
- Buckets → `ec-demo` → `Anonymous` → `Read Only` を許可

## 5. 動作確認

画像URLが 200 で取得できればOK。

```
http://<minio-host>:9000/ec-demo/products/1001/thumb.jpg
```

403 の場合:
- バケットが private のまま。Anonymous read を再確認。

404 の場合:
- オブジェクト未アップロード。CSVインポート時の imagesDir を確認。
