#!/bin/bash

# =============================================================================
# データベース初期化スクリプト (MySQL + MongoDB)
# =============================================================================
# 用途: platform/docker/demo/sql 配下の初期化スクリプトを実行
# - MySQL: mysql-init/*.sql
# - MongoDB: mongodb-init/*.js
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MYSQL_SQL_DIR="${SCRIPT_DIR}/sql/mysql-init"
MONGO_JS_DIR="${SCRIPT_DIR}/sql/mongodb-init"
ENV_FILE="${SCRIPT_DIR}/.env"

# カラー出力用
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}EC-Demo データベース初期化スクリプト${NC}"
echo -e "${GREEN}==============================================================================${NC}"

# .env ファイルの読み込み
if [ -f "${ENV_FILE}" ]; then
    echo -e "${GREEN}✓ .env ファイルを読み込み: ${ENV_FILE}${NC}"
    set -a
    source "${ENV_FILE}"
    set +a
fi

# =============================================================================
# MySQL 初期化
# =============================================================================
echo ""
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}[1/2] MySQL 初期化${NC}"
echo -e "${BLUE}==============================================================================${NC}"

# MySQL接続情報
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USERNAME:-root}"
MYSQL_PASS="${MYSQL_ROOT_PASSWORD:-123456}"
MYSQL_CONTAINER="ec-demo-mysql-8"

echo ""
echo -e "${YELLOW}MySQL接続情報:${NC}"
echo "  Host: ${MYSQL_HOST}"
echo "  Port: ${MYSQL_PORT}"
echo "  User: ${MYSQL_USER}"
echo ""

# MySQL接続確認
echo -e "${YELLOW}MySQL接続確認中...${NC}"
if ! docker exec ${MYSQL_CONTAINER} mysqladmin ping -h localhost -u root -p${MYSQL_PASS} --silent 2>/dev/null; then
    echo -e "${RED}エラー: MySQLに接続できません${NC}"
    echo -e "${YELLOW}MySQLコンテナが起動しているか確認してください:${NC}"
    echo "  docker ps | grep ec-demo-mysql"
    exit 1
fi
echo -e "${GREEN}✓ MySQL接続OK${NC}"
echo ""

# 実行するSQLファイル（順序通り）
SQL_FILES=(
    "00-create-dbs.sql"
    "01-seata-meta.sql"
    "10-business-ddl.sql"
    "11-add-personal-info-columns.sql"
    "20-seed-data.sql"
    "50-saga-ddl.sql"
    "60-alert-ddl.sql"
)

# SQLファイルを実行
for sql_file in "${SQL_FILES[@]}"; do
    sql_path="${MYSQL_SQL_DIR}/${sql_file}"

    if [ -f "${sql_path}" ]; then
        echo -e "${YELLOW}実行中: ${sql_file}${NC}"

        # SQLファイルをコンテナにコピーして実行
        docker cp "${sql_path}" ${MYSQL_CONTAINER}:/tmp/${sql_file}

        if docker exec ${MYSQL_CONTAINER} mysql -u root -p${MYSQL_PASS} -e "source /tmp/${sql_file}" 2>&1; then
            echo -e "${GREEN}✓ 完了: ${sql_file}${NC}"
        else
            echo -e "${RED}✗ 失敗: ${sql_file}${NC}"
            exit 1
        fi

        # 一時ファイル削除
        docker exec ${MYSQL_CONTAINER} rm -f /tmp/${sql_file}
    else
        echo -e "${RED}✗ ファイルが見つかりません: ${sql_path}${NC}"
        exit 1
    fi
done

echo ""
echo -e "${GREEN}MySQL データベース一覧:${NC}"
docker exec ${MYSQL_CONTAINER} mysql -u root -p${MYSQL_PASS} -e "SHOW DATABASES;" 2>/dev/null
echo -e "${GREEN}✓ MySQL 初期化完了${NC}"

# =============================================================================
# MongoDB 初期化
# =============================================================================
echo ""
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}[2/2] MongoDB 初期化${NC}"
echo -e "${BLUE}==============================================================================${NC}"

# MongoDB接続情報
MONGO_USER="${MONGO_ROOT_USER:-admin}"
MONGO_PASS="${MONGO_ROOT_PASSWORD:-admin123}"
MONGO_CONTAINER="ec-demo-mongodb-6"

echo ""
echo -e "${YELLOW}MongoDB接続情報:${NC}"
echo "  User: ${MONGO_USER}"
echo "  Container: ${MONGO_CONTAINER}"
echo ""

# MongoDBコンテナ確認
echo -e "${YELLOW}MongoDB接続確認中...${NC}"
if ! docker ps --format '{{.Names}}' | grep -q "^${MONGO_CONTAINER}$"; then
    echo -e "${YELLOW}⚠ MongoDBコンテナが起動していません。スキップします。${NC}"
    echo -e "${YELLOW}  MongoDBを起動するには: ./start-demo-env.sh mongo${NC}"
else
    if ! docker exec ${MONGO_CONTAINER} mongosh --eval "db.adminCommand('ping')" -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin --quiet 2>/dev/null; then
        echo -e "${RED}エラー: MongoDBに接続できません${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ MongoDB接続OK${NC}"
    echo ""

    # 実行するJSファイル
    MONGO_FILES=(
        "init-mongo.js"
        "update-validator.js"
    )

    # JSファイルを実行
    for js_file in "${MONGO_FILES[@]}"; do
        js_path="${MONGO_JS_DIR}/${js_file}"

        if [ -f "${js_path}" ]; then
            echo -e "${YELLOW}実行中: ${js_file}${NC}"

            # JSファイルをコンテナにコピーして実行
            docker cp "${js_path}" ${MONGO_CONTAINER}:/tmp/${js_file}

            if docker exec ${MONGO_CONTAINER} mongosh -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin --quiet /tmp/${js_file} 2>&1; then
                echo -e "${GREEN}✓ 完了: ${js_file}${NC}"
            else
                echo -e "${RED}✗ 失敗: ${js_file}${NC}"
                exit 1
            fi

            # 一時ファイル削除
            docker exec ${MONGO_CONTAINER} rm -f /tmp/${js_file}
        else
            echo -e "${YELLOW}⚠ スキップ: ${js_file} (ファイルが見つかりません)${NC}"
        fi
    done

    echo ""
    echo -e "${GREEN}MongoDB コレクション一覧 (ec_demo):${NC}"
    docker exec ${MONGO_CONTAINER} mongosh -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin --quiet --eval "use ec_demo; db.getCollectionNames()" 2>/dev/null
    echo -e "${GREEN}✓ MongoDB 初期化完了${NC}"
fi

# =============================================================================
# 完了
# =============================================================================
echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}✓ データベース初期化が完了しました${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo ""
