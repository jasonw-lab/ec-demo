#!/usr/bin/env bash

COUNT="${1:-1}"
PRODUCT_ID="${2:-1}"
API_BASE="${API_BASE:-http://127.0.0.1:8080}"
CUSTOMER_NAME="${CUSTOMER_NAME:-Demo Taro}"
CUSTOMER_EMAIL="${CUSTOMER_EMAIL:-demo@example.com}"

curl -L -X POST "${API_BASE}/api/orders/purchase" \
  -H 'Content-Type: application/json' \
  --data "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"customerEmail\": \"${CUSTOMER_EMAIL}\",
    \"items\": [
      { \"productId\": ${PRODUCT_ID}, \"quantity\": ${COUNT} }
    ]
  }"
