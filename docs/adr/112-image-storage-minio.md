# Title
画像ストレージにMinIOを採用する

## Status (Proposed / Accepted / Deprecated)
Accepted

## Context
- 商品画像などのバイナリファイルをストレージに保存し、フロントエンドから直接アクセスできる必要がある。
- Elasticsearchでは商品情報のみを管理し、画像URLを保持することで検索性能とストレージコストを最適化したい。
- ローカル開発環境でも本番環境と同様のオブジェクトストレージAPIを使用して、環境差異を最小化したい。
- **デモ実装において、画像アップロードとサムネイル生成機能を迅速に実装し、E-Cサイトのリアルな体験を提供したい。**

## Decision
- MinIO (バージョン: 8.5.7) をオブジェクトストレージとして採用する。
- MinIOはS3互換APIを提供し、将来的にAWS S3への移行が容易である。
- es-serviceでMinIOClient（Java SDK）を使用し、画像アップロード・バケット管理を行う。
- アップロード時にオリジナル画像とサムネイル画像の両方を保存し、公開URLをElasticsearchに格納する。
- Docker Composeでローカル環境にMinIOコンテナを起動し、開発環境でも本番同様のストレージ体験を実現する。

## Alternatives
- **AWS S3** - 本番環境では最適だが、ローカル開発でのセットアップが複雑。AWSアカウント管理やコスト管理が必要。開発初期段階ではオーバースペック。
- **ローカルファイルシステム** - 実装は簡単だが、マイクロサービス間でのファイル共有が困難。本番環境との環境差異が大きく、URLの生成やアクセス制御の実装が複雑になる。
- **Azure Blob Storage** - S3同様にクラウドサービスであり、ローカル開発でのエミュレーション環境が必要。
- **MongoDB GridFS** - バイナリデータの保存は可能だが、HTTPでの直接アクセスができず、専用のダウンロードエンドポイントが必要。画像配信のパフォーマンスが劣る。

## Consequences
- **メリット**:
  - S3互換APIにより、将来的なクラウド移行（AWS S3、Azure Blob Storage等）が容易。
  - ローカル開発環境で本番同様のオブジェクトストレージ体験を提供。
  - Docker Composeで簡単にセットアップでき、開発者のオンボーディングが迅速。
  - バケット・オブジェクト管理がシンプルで、HTTPでの直接アクセスが可能。
  - 画像データとメタデータ（Elasticsearch）を分離することで、検索パフォーマンスとストレージコストを最適化。
  
- **デメリット**:
  - ローカル環境ではMinIOの運用管理が必要（Dockerコンテナの起動・停止）。
  - MinIOの可用性やバケット設定に依存するため、設定ミスによる障害のリスクがある。
  - 本番環境で別のストレージサービスに切り替える場合は、設定変更とデプロイが必要。

## Version
- MinIO Java SDK: 8.5.7
- MinIO Server: latest (Docker image: minio/minio)

## References
- [MinIO Documentation](https://min.io/docs/minio/linux/index.html)
- [MinIO Java SDK](https://github.com/minio/minio-java)
- [AWS S3 Compatibility](https://min.io/product/s3-compatibility)

