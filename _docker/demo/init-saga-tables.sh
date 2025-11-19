#!/bin/bash
# Script to initialize SAGA tables in MySQL
# Usage: ./init-saga-tables.sh [container_name]
# Default container name: ec-demo-mysql-8

CONTAINER_NAME=${1:-ec-demo-mysql-8}
SQL_FILE="$(dirname "$0")/sql/mysql-init/50-saga-ddl.sql"

echo "Initializing SAGA tables in MySQL container: $CONTAINER_NAME"
echo "SQL file: $SQL_FILE"

if [ ! -f "$SQL_FILE" ]; then
    echo "Error: SQL file not found at $SQL_FILE"
    exit 1
fi

if ! docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
    echo "Error: Container '$CONTAINER_NAME' is not running"
    echo "Available MySQL containers:"
    docker ps --filter "ancestor=mysql" --format "  {{.Names}}"
    exit 1
fi

echo "Executing SAGA DDL..."
docker exec -i "$CONTAINER_NAME" mysql -uroot -p123456 < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo "✓ SAGA tables initialized successfully"
else
    echo "✗ Failed to initialize SAGA tables"
    exit 1
fi

