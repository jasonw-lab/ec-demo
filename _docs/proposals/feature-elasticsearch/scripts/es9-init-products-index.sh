#!/usr/bin/env bash
set -euo pipefail

ES_BASE_URL="${ES_BASE_URL:-${ELASTICSEARCH_ENDPOINT:-http://localhost:${ES_HTTP_PORT:-9200}}}"
INDEX_VERSIONED="${INDEX_VERSIONED:-products_v1}"
ALIAS="${ALIAS:-products}"

echo "Using ES_BASE_URL=${ES_BASE_URL}"
echo "Creating index=${INDEX_VERSIONED} and alias=${ALIAS}"

# Create versioned index (idempotent-ish: skip if already exists)
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
  echo
fi

# Point alias to the versioned index (remove from others)
curl -fsS -X POST "${ES_BASE_URL}/_aliases" \
  -H 'Content-Type: application/json' \
  -d @- <<JSON
{
  "actions": [
    { "remove": { "index": "products_*", "alias": "${ALIAS}", "ignore_unavailable": true } },
    { "add": { "index": "${INDEX_VERSIONED}", "alias": "${ALIAS}" } }
  ]
}
JSON
echo

echo "Done. Alias '${ALIAS}' -> '${INDEX_VERSIONED}'"
