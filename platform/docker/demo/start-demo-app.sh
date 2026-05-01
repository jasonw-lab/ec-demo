#!/bin/bash

# =============================================================================
# docker-compose-demo-app.yml 起動スクリプト
# =============================================================================
# 用途: EC-Demoアプリケーションコンテナを起動
# 含まれるサービス:
#   - account-service, storage-service, order-service, payment-service
#   - bff, front (nginx)
#   - es-service (profile: elastic)
# 前提: docker-compose-demo-env.yml が起動済みであること
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-demo-app.yml"
ENV_FILE="${SCRIPT_DIR}/.env"

# カラー出力用
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}EC-Demo アプリケーション起動スクリプト${NC}"
echo -e "${GREEN}==============================================================================${NC}"

# .env ファイルの読み込み
if [ -f "${ENV_FILE}" ]; then
    echo -e "${GREEN}✓ .env ファイルを読み込み: ${ENV_FILE}${NC}"
    set -a
    source "${ENV_FILE}"
    set +a
fi

# BASEPATH のデフォルト値設定
BASEPATH="${BASEPATH:-/Users/wangjw/Dev/_Env/_demo/seata-mode}"
export BASEPATH

# DATASOURCE_PASSWORD のデフォルト値設定
DATASOURCE_PASSWORD="${DATASOURCE_PASSWORD:-${MYSQL_ROOT_PASSWORD:-123456}}"
SAGA_DATASOURCE_PASSWORD="${SAGA_DATASOURCE_PASSWORD:-${MYSQL_ROOT_PASSWORD:-123456}}"
export DATASOURCE_PASSWORD SAGA_DATASOURCE_PASSWORD

# 環境変数の表示
echo ""
echo -e "${YELLOW}使用する環境変数:${NC}"
echo "  BASEPATH: ${BASEPATH}"
echo ""

# ネットワーク存在確認
if ! docker network inspect jason-lab-net >/dev/null 2>&1; then
    echo -e "${RED}エラー: ネットワーク 'jason-lab-net' が存在しません${NC}"
    echo -e "${YELLOW}先に docker-compose-demo-env.yml を起動してください:${NC}"
    echo -e "  ./start-demo-env.sh"
    exit 1
fi
echo -e "${GREEN}✓ ネットワーク 'jason-lab-net' を確認${NC}"

# docker-compose起動
echo ""
echo -e "${GREEN}docker-compose を起動します...${NC}"
echo "  Compose file: ${COMPOSE_FILE}"
echo ""

# プロファイルの指定（オプション）
PROFILES=""
if [ -n "$1" ]; then
    PROFILES="--profile $1"
    shift
    while [ -n "$1" ]; do
        PROFILES="$PROFILES --profile $1"
        shift
    done
fi

docker compose -f "${COMPOSE_FILE}" ${PROFILES} up -d --build

# 起動確認
echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}起動完了チェック${NC}"
echo -e "${GREEN}==============================================================================${NC}"
docker compose -f "${COMPOSE_FILE}" ${PROFILES} ps

# 接続情報の表示
echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}サービス接続情報${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo -e "BFF:              ${YELLOW}http://localhost:18080${NC}"
echo -e "Order Service:    ${YELLOW}http://localhost:18081${NC}"
echo -e "Storage Service:  ${YELLOW}http://localhost:18082${NC}"
echo -e "Account Service:  ${YELLOW}http://localhost:18083${NC}"
echo -e "Payment Service:  ${YELLOW}http://localhost:18090${NC}"
echo -e "Frontend:         ${YELLOW}http://localhost:18000${NC}"
echo ""
echo -e "${YELLOW}プロファイル別サービス:${NC}"
echo -e "  --profile elastic:"
echo -e "    ES Service:   ${YELLOW}http://localhost:8086${NC}"
echo ""
echo -e "${GREEN}使用例:${NC}"
echo -e "  ./start-demo-app.sh              # 基本サービスのみ起動"
echo -e "  ./start-demo-app.sh elastic      # es-service も起動"
echo ""
echo -e "${GREEN}✓ アプリケーションが起動しました${NC}"
echo ""
