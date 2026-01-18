#!/bin/bash
# MongoDB order_audit バリデーションルール更新APIのテストスクリプト

# デフォルト設定
ORDER_SERVICE_HOST="${1:-localhost}"
ORDER_SERVICE_PORT="${2:-8082}"
API_URL="http://${ORDER_SERVICE_HOST}:${ORDER_SERVICE_PORT}/api/admin/order-audit/update-validator"

echo "=================================================="
echo "  MongoDB Validator Update API Test"
echo "=================================================="
echo "Target: ${API_URL}"
echo ""

# APIを呼び出し
echo "🚀 Calling API..."
response=$(curl -s -X POST "${API_URL}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP_STATUS:%{http_code}")

# HTTPステータスコードを抽出
http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
body=$(echo "$response" | sed '/HTTP_STATUS/d')

echo ""
echo "📊 Response:"
echo "----------------------------------------"
echo "HTTP Status: ${http_status}"
echo ""
echo "Body:"
echo "${body}" | jq '.' 2>/dev/null || echo "${body}"
echo "----------------------------------------"
echo ""

# 結果判定
if [ "${http_status}" = "200" ]; then
    success=$(echo "${body}" | jq -r '.success' 2>/dev/null)
    if [ "${success}" = "true" ]; then
        echo "✅ SUCCESS: Validator updated successfully"
        exit 0
    else
        echo "⚠️  WARNING: API returned 200 but success=false"
        exit 1
    fi
elif [ "${http_status}" = "500" ]; then
    echo "❌ ERROR: Internal Server Error (500)"
    exit 1
else
    echo "❌ ERROR: Unexpected HTTP status ${http_status}"
    exit 1
fi
