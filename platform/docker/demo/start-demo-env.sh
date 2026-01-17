#!/bin/bash

# =============================================================================
# docker-compose-demo-env.yml 起動スクリプト
# =============================================================================
# 用途: EC-Demo環境の全インフラストラクチャコンテナを起動
# 含まれるサービス:
#   - MySQL, Seata Server, Kafka, Kafka UI, Elasticsearch, Kibana, MinIO, MongoDB
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-demo-env.yml"

# カラー出力用
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}EC-Demo 環境起動スクリプト${NC}"
echo -e "${GREEN}==============================================================================${NC}"

# HOST_IP を自動検出 (外部ネットワークへのルートから取得)
if [ -z "${HOST_IP}" ]; then
    # Linux/macOS で動作するIPアドレス検出
    # 1.1.1.1への経路から自ホストのIPアドレスを取得
    DETECTED_IP="$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' | head -n1)"
    
    # macOSの場合は別の方法を試す
    if [ -z "${DETECTED_IP}" ]; then
        DETECTED_IP="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "")"
    fi
    
    if [ -n "${DETECTED_IP}" ]; then
        export HOST_IP="${DETECTED_IP}"
        echo -e "${GREEN}✓ HOST_IP を自動検出: ${HOST_IP}${NC}"
    else
        export HOST_IP="localhost"
        echo -e "${YELLOW}⚠ HOST_IP を検出できませんでした。デフォルト値 'localhost' を使用します${NC}"
    fi
else
    echo -e "${GREEN}✓ HOST_IP は既に設定済み: ${HOST_IP}${NC}"
fi

# 環境変数の表示
echo ""
echo -e "${YELLOW}使用する環境変数:${NC}"
echo "  HOST_IP: ${HOST_IP}"
echo "  EC_DEMO_NETWORK: ${EC_DEMO_NETWORK:-docker_youlai-boot}"
echo "  EC_DEMO_BASEPATH: ${EC_DEMO_BASEPATH:-/mydata/ec-demo}"
echo ""

# # Dockerネットワークの存在確認と作成
# NETWORK_NAME="${EC_DEMO_NETWORK:-docker_youlai-boot}"
# if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
#     echo -e "${YELLOW}⚠ Docker network '${NETWORK_NAME}' が存在しません。作成します...${NC}"
#     docker network create "${NETWORK_NAME}"
#     echo -e "${GREEN}✓ Docker network '${NETWORK_NAME}' を作成しました${NC}"
# else
#     echo -e "${GREEN}✓ Docker network '${NETWORK_NAME}' は既に存在します${NC}"
# fi

# docker-compose起動
echo ""
echo -e "${GREEN}docker-compose を起動します...${NC}"
echo "  Compose file: ${COMPOSE_FILE}"
echo ""

docker compose -f "${COMPOSE_FILE}" up -d

# 起動確認
echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}起動完了チェック${NC}"
echo -e "${GREEN}==============================================================================${NC}"
docker compose -f "${COMPOSE_FILE}" ps

# 接続情報の表示
echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}サービス接続情報${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo -e "MySQL:          ${YELLOW}localhost:3307${NC} (root/123456)"
echo -e "Seata Console:  ${YELLOW}http://localhost:7092${NC}"
echo -e "Seata Server:   ${YELLOW}localhost:8091${NC}"
echo -e "Kafka (外部):   ${YELLOW}${HOST_IP}:29092${NC}"
echo -e "Kafka (内部):   ${YELLOW}ec-demo-kafka:9092${NC}"
echo -e "Kafka UI:       ${YELLOW}http://localhost:8090${NC}"
echo -e "Elasticsearch:  ${YELLOW}http://localhost:9200${NC}"
echo -e "Kibana:         ${YELLOW}http://localhost:5601${NC}"
echo -e "MinIO Console:  ${YELLOW}http://localhost:9001${NC} (minioadmin/minioadmin123)"
echo -e "MinIO API:      ${YELLOW}http://localhost:9000${NC}"
echo -e "MongoDB:        ${YELLOW}localhost:27017${NC}"
echo ""
echo -e "${GREEN}✓ すべてのサービスが起動しました${NC}"
echo ""
