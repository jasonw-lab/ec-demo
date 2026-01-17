# ec-demo デプロイ手順書

既存のsmart-retail環境にec-demoを追加デプロイする手順です。

## ファイル構成

ec-demoのDocker Composeファイルは以下の2つに分かれています：

- **`docker-compose-demo-env.yml`**: ミドルウェア（MySQL、Seata）
- **`docker-compose-demo-app.yml`**: アプリケーションサービス（account-service、storage-service、order-service、BFF、front）

この構成により、ミドルウェアとアプリケーションを独立して管理できます。

## 前提条件

- 既存smart-retailが `youlai-boot` ネットワークで稼働中
- nginx設定ディレクトリ: `/mydata/nginx/conf/conf.d/`
- VPS環境: Ubuntu
- Docker、Docker Composeがインストール済み
- Maven 3.9+、Java 17がインストール済み（ビルド用）

## デプロイフロー

### ステップ1: ファイル配置

```bash
# VPS上でec-demoリポジトリをクローン/配置
cd /path/to
git clone <repository-url> ec-demo
cd ec-demo
git checkout feature/transacion

# docker-composeファイル確認
cd platform/docker/demo
ls -la docker-compose-demo-env.yml docker-compose-demo-app.yml
```

### ステップ2: データディレクトリ作成

```bash
# 永続化ディレクトリ作成
sudo mkdir -p /mydata/ec-demo/{mysql/{data,conf,log},seata/{logs,conf},app/{account-service,storage-service,order-service,bff}/logs,front/dist}

# MySQL設定ファイル（既存のmy.cnfをコピーまたは新規作成）
sudo cp /mydata/mysql/conf/my.cnf /mydata/ec-demo/mysql/conf/my.cnf 2>/dev/null || \
sudo tee /mydata/ec-demo/mysql/conf/my.cnf > /dev/null <<EOF
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
EOF

# 所有者変更
sudo chown -R $USER:$USER /mydata/ec-demo
```

### ステップ3: Seata設定ファイル作成

```bash
# Seata設定ディレクトリ確認
sudo mkdir -p /mydata/ec-demo/seata/conf

# Seata application.yml作成（基本設定）
sudo tee /mydata/ec-demo/seata/conf/application.yml > /dev/null <<'EOF'
server:
  port: 7091

spring:
  application:
    name: seata-server

seata:
  config:
    type: file
    file:
      name: file.conf
  registry:
    type: file
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://ec-demo-mysql:3306/seata?rewriteBatchedStatements=true
      user: root
      password: 123456
      min-conn: 10
      max-conn: 100
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
      query-limit: 1000
      max-wait: 5000
EOF

# file.conf作成
sudo tee /mydata/ec-demo/seata/conf/file.conf > /dev/null <<'EOF'
transport {
  type = TCP
  server = NIO
  heartbeat = true
  enableClientBatchSendRequest = true
}

service {
  vgroupMapping.saga_tx_group = default
  default.grouplist = ec-demo-seata-server:8091
  enableDegrade = false
  disableGlobalTransaction = false
}

client {
  rm.asyncCommitBufferLimit = 10000
  rm.reportRetryCount = 5
  rm.tableMetaCheckEnable = false
  rm.reportSuccessEnable = false
  rm.sagaBranchRegisterEnable = false
  rm.sagaJsonParser = fastjson
  rm.sagaRetryPersistModeUpdate = false
  rm.sagaRetryPeriod = 1000
  rm.sagaCompensatePersistModeUpdate = false
  tm.commitRetryCount = 5
  tm.rollbackRetryCount = 5
  tm.degradeCheck = false
  tm.degradeCheckAllowTimes = 10
  tm.degradeCheckPeriod = 2000
  undo.dataValidation = true
  undo.logSerialization = jackson
  undo.onlyCareUpdateColumns = true
  undo.logTable = undo_log
  undo.compress.enable = true
  undo.compress.type = zip
  undo.compress.threshold = 64k
  log.exceptionRate = 100
}
EOF
```

### ステップ4: フロントエンドビルド

```bash
# フロントエンドディレクトリに移動
cd /path/to/ec-demo/apps/web

# 依存関係インストール
npm install

# ビルド
npm run build

# ビルド成果物を既存nginx用ディレクトリにコピー（本番用）
sudo mkdir -p /mydata/nginx/html/ec-demo
sudo cp -r dist/* /mydata/nginx/html/ec-demo/

# デバッグ用ディレクトリにもコピー（オプション）
sudo mkdir -p /mydata/ec-demo/front/dist
sudo cp -r dist/* /mydata/ec-demo/front/dist/
```

### ステップ5: Docker Compose起動

```bash
# docker-composeディレクトリに移動
cd /path/to/ec-demo/platform/docker/demo

# ミドルウェア起動（MySQL、Seata）
docker compose -f docker-compose-demo-env.yml up -d

# 起動確認
docker compose -f docker-compose-demo-env.yml ps

# アプリケーションサービス起動（account-service、storage-service、order-service、BFF、front）
docker compose -f docker-compose-demo-app.yml up -d --build

# 起動確認
docker compose -f docker-compose-demo-app.yml ps

# ログ確認（問題がある場合）
docker compose -f docker-compose-demo-env.yml logs -f
docker compose -f docker-compose-demo-app.yml logs -f
```

### ステップ6: nginx設定追加

```bash
# ec-demo設定ファイル作成
sudo tee /mydata/nginx/conf/conf.d/ec-demo.conf > /dev/null <<'EOF'
# ec-demo frontend
location /ec-demo/ {
    alias /usr/share/nginx/html/ec-demo/;
    index index.html;
    try_files $uri $uri/ /ec-demo/index.html;
}

# ec-demo BFF API
location /ec-api/ {
    proxy_pass http://ec-demo-bff:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # CORS設定（必要に応じて）
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS, PUT, DELETE' always;
    add_header Access-Control-Allow-Headers 'Origin, Authorization, Content-Type, Accept' always;
    
    # OPTIONSプリフライト対応
    if ($request_method = OPTIONS) {
        add_header Access-Control-Max-Age 1728000;
        add_header Content-Type 'text/plain charset=UTF-8';
        add_header Content-Length 0;
        return 204;
    }
}

# ec-demo WebSocket
location /ec-api/ws/ {
    proxy_pass http://ec-demo-bff:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocketタイムアウト設定
    proxy_read_timeout 86400;
    proxy_send_timeout 86400;
}
EOF

# default.confにec-demo設定のincludeを追加（既に追加済みの場合はスキップ）
# default.confのserverブロック内、error_pageの前に以下を追加:
#   # ec-demo configuration
#   # 分離可能: この行をコメントアウトまたは削除することでec-demo設定を無効化できます
#   include ec-demo.conf;
#
# 既存のdefault.confを確認し、include ec-demo.conf;が無い場合は追加
if ! grep -q "include ec-demo.conf;" /mydata/nginx/conf/conf.d/default.conf; then
    # error_pageの前にincludeを追加
    sudo sed -i '/# error page handling/i\    # ec-demo configuration\n    # 分離可能: この行をコメントアウトまたは削除することでec-demo設定を無効化できます\n    include ec-demo.conf;\n' /mydata/nginx/conf/conf.d/default.conf
fi

# nginx設定テスト
sudo docker exec youlai-nginx nginx -t

# nginx再読み込み（既存サービス影響なし）
sudo docker exec youlai-nginx nginx -s reload
```

### ステップ7: 動作確認

```bash
# サービスヘルスチェック（デバッグ用ポート）
curl http://localhost:18083/actuator/health  # account-service
curl http://localhost:18081/actuator/health  # order-service
curl http://localhost:18082/actuator/health  # storage-service
curl http://localhost:18080/actuator/health  # BFF

# nginx経由アクセステスト
curl http://localhost/ec-api/actuator/health
curl http://localhost/ec-demo/

# MySQL接続確認
docker exec -it ec-demo-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"

# Seataコンソール確認
curl http://localhost:7092
```

## ロールバック手順（問題発生時）

```bash
# ec-demoコンテナ停止
cd /path/to/ec-demo/platform/docker/demo

# アプリケーションサービス停止
docker compose -f docker-compose-demo-app.yml down

# ミドルウェア停止（必要に応じて）
docker compose -f docker-compose-demo-env.yml down

# nginx設定削除（2つの方法）
# 方法1: ec-demo.confファイルを削除
sudo rm /mydata/nginx/conf/conf.d/ec-demo.conf

# 方法2: default.confからinclude行をコメントアウトまたは削除
# sudo sed -i 's/^[[:space:]]*include ec-demo.conf;/# include ec-demo.conf;/' /mydata/nginx/conf/conf.d/default.conf

# nginx再読み込み
sudo docker exec youlai-nginx nginx -s reload

# データ削除（必要に応じて）
# sudo rm -rf /mydata/ec-demo
```

## トラブルシューティング

### MySQLが起動しない

```bash
# ログ確認
docker logs ec-demo-mysql

# データディレクトリの権限確認
ls -la /mydata/ec-demo/mysql/data
```

### Seataが起動しない

```bash
# ログ確認
docker logs ec-demo-seata-server

# 設定ファイル確認
cat /mydata/ec-demo/seata/conf/application.yml
```

### サービスが起動しない

```bash
# 各サービスのログ確認
docker logs ec-demo-account-service
docker logs ec-demo-storage-service
docker logs ec-demo-order-service
docker logs ec-demo-bff

# ネットワーク確認
docker network inspect youlai-boot
```

### nginx設定エラー

```bash
# nginx設定テスト
sudo docker exec youlai-nginx nginx -t

# nginxログ確認
sudo docker exec youlai-nginx tail -f /var/log/nginx/error.log
```

## ポート一覧

| サービス | ホストポート | コンテナ内ポート | 用途 |
|---------|------------|----------------|------|
| ec-demo-mysql | 3307 | 3306 | ec-demo専用DB |
| ec-demo-seata | 8092 | 8091 | ec-demo専用Seata |
| ec-demo-seata-console | 7092 | 7091 | Seataコンソール |
| ec-demo-account-service | 18083 | 8083 | デバッグ用 |
| ec-demo-order-service | 18081 | 8081 | デバッグ用 |
| ec-demo-storage-service | 18082 | 8082 | デバッグ用 |
| ec-demo-bff | 18080 | 8080 | デバッグ用 |
| ec-demo-front | 18000 | 80 | デバッグ用 |

**本番アクセス**: nginx経由（ポート80/443）

- フロントエンド: `http://<domain>/ec-demo/`
- BFF API: `http://<domain>/ec-api/`
- WebSocket: `ws://<domain>/ec-api/ws/`

## 更新手順

### サービス更新

```bash
cd /path/to/ec-demo/platform/docker/demo

# コード更新
cd ../..
git pull origin feature/transacion

# アプリケーションサービス再ビルド＆再起動
docker compose -f docker-compose-demo-app.yml up -d --build

# ミドルウェア更新（必要に応じて）
# docker compose -f docker-compose-demo-env.yml up -d
```

### フロントエンド更新

```bash
cd /path/to/ec-demo/apps/web
npm install
npm run build
# 既存nginx用ディレクトリにコピー
sudo cp -r dist/* /mydata/nginx/html/ec-demo/
```

## 注意事項

- 既存のsmart-retailサービスには影響しません
- ec-demoは独立したMySQL、Seataインスタンスを使用します
- ネットワークは既存の`youlai-boot`を再利用します
- ポート衝突は回避済み（既存サービスと異なるポートを使用）

## nginx設定の分離方法

ec-demoのnginx設定は`default.conf`から`include ec-demo.conf;`で読み込まれています。分離するには以下の方法があります：

### 方法1: include行をコメントアウト

```bash
# default.confを編集
sudo vi /mydata/nginx/conf/conf.d/default.conf

# 以下の行をコメントアウト:
#   include ec-demo.conf;
# ↓
#   # include ec-demo.conf;

# nginx再読み込み
sudo docker exec youlai-nginx nginx -s reload
```

### 方法2: ec-demo.confファイルを削除

```bash
# ec-demo.confファイルを削除
sudo rm /mydata/nginx/conf/conf.d/ec-demo.conf

# nginx再読み込み
sudo docker exec youlai-nginx nginx -s reload
```

### 方法3: ec-demo.confファイルをリネーム

```bash
# ec-demo.confを無効化（.conf以外の拡張子に変更）
sudo mv /mydata/nginx/conf/conf.d/ec-demo.conf /mydata/nginx/conf/conf.d/ec-demo.conf.disabled

# nginx再読み込み
sudo docker exec youlai-nginx nginx -s reload
```

**推奨**: 方法1（include行のコメントアウト）が最も簡単で、後で再有効化しやすいです。
