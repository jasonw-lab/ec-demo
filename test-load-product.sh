#!/bin/bash

# es-service Import API 実行スクリプト
# Usage: ./load-products.sh [csv_path] [images_dir]

set -e

# デフォルト値
CSV_PATH="${1:-_docs/docker/demo/elasticsearch/data/product/sample-products.csv}"
IMAGES_DIR="${2:-_docs/docker/demo/elasticsearch/data/product/images}"
ES_SERVICE_URL="${ES_SERVICE_URL:-http://localhost:8086}"

echo "=========================================="
echo "es-service Import API 実行"
echo "=========================================="
echo "CSV Path: ${CSV_PATH}"
echo "Images Dir: ${IMAGES_DIR}"
echo "API URL: ${ES_SERVICE_URL}/internal/products/import"
echo ""

# Import API呼び出し
echo "🚀 Import APIを呼び出しています..."
RESPONSE=$(curl -s -X POST "${ES_SERVICE_URL}/internal/products/import" \
  -H "Content-Type: application/json" \
  -d "{\"csvPath\":\"${CSV_PATH}\",\"imagesDir\":\"${IMAGES_DIR}\"}" \
  -w "\n%{http_code}")

# レスポンスコードを分離
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo ""
echo "📊 結果:"
echo "HTTP Status: ${HTTP_CODE}"
echo "Response Body:"
echo "${BODY}" | jq . 2>/dev/null || echo "${BODY}"

if [ "${HTTP_CODE}" = "200" ]; then
  echo ""
  echo "✅ Import成功"
  exit 0
else
  echo ""
  echo "❌ Import失敗 (HTTP ${HTTP_CODE})"
  exit 1
fi
