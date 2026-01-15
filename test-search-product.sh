#!/bin/bash

# es-service Search API 実行スクリプト
# Usage:
#   ./test-search-product.sh [q] [minPrice] [maxPrice] [sort] [page] [size]
#
# 例:
#   ./test-search-product.sh "iPhone" 10000 200000 price_asc 0 20

set -e

# デフォルト値
Q="${1:-}"
MIN_PRICE="${2:-}"
MAX_PRICE="${3:-}"
SORT="${4:-relevance}"
PAGE="${5:-0}"
SIZE="${6:-20}"
ES_SERVICE_URL="${ES_SERVICE_URL:-http://localhost:8086}"

echo "=========================================="
echo "es-service Search API 実行"
echo "=========================================="
echo "Query (q):        ${Q}"
echo "Min Price:        ${MIN_PRICE}"
echo "Max Price:        ${MAX_PRICE}"
echo "Sort:             ${SORT}"
echo "Page:             ${PAGE}"
echo "Size:             ${SIZE}"
echo "API URL:          ${ES_SERVICE_URL}/api/search/products"
echo ""

# クエリパラメータ組み立て
PARAMS=()
if [ -n "${Q}" ]; then
  PARAMS+=("q=$(printf '%s' "${Q}" | jq -s -R -r @uri)")
fi
if [ -n "${MIN_PRICE}" ]; then
  PARAMS+=("minPrice=${MIN_PRICE}")
fi
if [ -n "${MAX_PRICE}" ]; then
  PARAMS+=("maxPrice=${MAX_PRICE}")
fi
if [ -n "${SORT}" ]; then
  PARAMS+=("sort=$(printf '%s' "${SORT}" | jq -s -R -r @uri)")
fi
PARAMS+=("page=${PAGE}")
PARAMS+=("size=${SIZE}")

QUERY_STRING=$(IFS='&'; echo "${PARAMS[*]}")

URL="${ES_SERVICE_URL}/api/search/products"
if [ -n "${QUERY_STRING}" ]; then
  URL="${URL}?${QUERY_STRING}"
fi

echo "🚀 Search APIを呼び出しています..."
echo "Request URL: ${URL}"

RESPONSE=$(curl -s -X GET "${URL}" \
  -H "Accept: application/json" \
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
  echo "✅ Search成功"
  exit 0
else
  echo ""
  echo "❌ Search失敗 (HTTP ${HTTP_CODE})"
  exit 1
fi

