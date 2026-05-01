#!/usr/bin/env bash
# =============================================================================
# Initialize Elasticsearch products index and import sample data
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ES_BASE_URL="${ES_BASE_URL:-${ELASTICSEARCH_ENDPOINT:-http://localhost:${ES_HTTP_PORT:-9200}}}"
INDEX_VERSIONED="${INDEX_VERSIONED:-products_v1}"
ALIAS="${ALIAS:-products}"
CSV_FILE="${CSV_FILE:-${SCRIPT_DIR}/data/product/sample-products.csv}"
MINIO_BASE_URL="${MINIO_BASE_URL:-http://localhost:9000}"
MINIO_BUCKET="${MINIO_BUCKET:-ec-demo}"
MINIO_PATH="${MINIO_PATH:-product/images}"

echo "============================================="
echo "Elasticsearch Products Index Setup"
echo "============================================="
echo "ES_BASE_URL:     ${ES_BASE_URL}"
echo "INDEX_VERSIONED: ${INDEX_VERSIONED}"
echo "ALIAS:           ${ALIAS}"
echo "CSV_FILE:        ${CSV_FILE}"
echo "MINIO_BASE_URL:  ${MINIO_BASE_URL}"
echo "============================================="

# =============================================================================
# 1. Create index
# =============================================================================
echo ""
echo "[1/4] Creating index '${INDEX_VERSIONED}'..."

if curl -fsS "${ES_BASE_URL}/${INDEX_VERSIONED}" >/dev/null 2>&1; then
  echo "Index already exists: ${INDEX_VERSIONED} (skip)"
else
  curl -fsS -X PUT "${ES_BASE_URL}/${INDEX_VERSIONED}" \
    -H 'Content-Type: application/json' \
    -d @- <<'JSON'
{
  "mappings": {
    "properties": {
      "productId": { "type": "long" },
      "title": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword", "ignore_above": 256 } }
      },
      "description": { "type": "text" },
      "price": { "type": "long" },
      "status": { "type": "keyword" },
      "thumbnailUrl": { "type": "keyword" },
      "createdAt": { "type": "date" }
    }
  }
}
JSON
  echo " -> created"
fi

# =============================================================================
# 2. Set alias
# =============================================================================
echo ""
echo "[2/4] Setting alias '${ALIAS}' -> '${INDEX_VERSIONED}'..."

curl -fsS -X POST "${ES_BASE_URL}/_aliases" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "actions": [
    { "add": { "index": "${INDEX_VERSIONED}", "alias": "${ALIAS}" } }
  ]
}
JSON
echo " -> done"

# =============================================================================
# 3. Import products from CSV
# =============================================================================
echo ""
echo "[3/4] Importing products from CSV..."

if [ ! -f "${CSV_FILE}" ]; then
  echo "WARNING: CSV file not found: ${CSV_FILE} (skip import)"
else
  # Build thumbnail URL base
  THUMBNAIL_BASE="${MINIO_BASE_URL}/${MINIO_BUCKET}/${MINIO_PATH}"

  # Read CSV and bulk import (skip header)
  BULK_DATA=""
  IMPORTED=0

  while IFS=',' read -r productId title description price status createdAt imageFile; do
    # Skip header row
    if [ "$productId" = "productId" ]; then
      continue
    fi

    # Build thumbnail URL
    thumbnailUrl="${THUMBNAIL_BASE}/${imageFile}"

    # Escape special characters in JSON strings
    title_escaped=$(echo "$title" | sed 's/"/\\"/g')
    description_escaped=$(echo "$description" | sed 's/"/\\"/g')

    # Build bulk action
    BULK_DATA+='{"index":{"_index":"'"${INDEX_VERSIONED}"'","_id":"'"${productId}"'"}}'
    BULK_DATA+=$'\n'
    BULK_DATA+='{"productId":'"${productId}"',"title":"'"${title_escaped}"'","description":"'"${description_escaped}"'","price":'"${price}"',"status":"'"${status}"'","thumbnailUrl":"'"${thumbnailUrl}"'","createdAt":"'"${createdAt}"'"}'
    BULK_DATA+=$'\n'

    IMPORTED=$((IMPORTED + 1))
  done < "${CSV_FILE}"

  # Execute bulk import
  if [ -n "${BULK_DATA}" ]; then
    RESULT=$(echo -e "${BULK_DATA}" | curl -fsS -X POST "${ES_BASE_URL}/_bulk" \
      -H 'Content-Type: application/x-ndjson' \
      --data-binary @-)

    # Check for errors
    if echo "${RESULT}" | grep -q '"errors":true'; then
      echo "WARNING: Some documents failed to import"
      echo "${RESULT}" | head -500
    else
      echo " -> imported ${IMPORTED} products"
    fi
  fi
fi

# =============================================================================
# 4. Verify
# =============================================================================
echo ""
echo "[4/4] Verifying..."

DOC_COUNT=$(curl -fsS "${ES_BASE_URL}/${INDEX_VERSIONED}/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
echo "Document count: ${DOC_COUNT}"

echo ""
echo "============================================="
echo "Setup complete!"
echo "  Index: ${INDEX_VERSIONED}"
echo "  Alias: ${ALIAS}"
echo "  Documents: ${DOC_COUNT}"
echo "============================================="
