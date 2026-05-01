#!/bin/bash
# =============================================================================
# Setup script for Docker environment
# Copies config files to BASEPATH if they don't exist
# =============================================================================

set -e

# Default BASEPATH
BASEPATH="${BASEPATH:-/Users/wangjw/Dev/_Env/_demo/seata-mode}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_SRC="${SCRIPT_DIR}/conf"

echo "Setting up Docker environment..."
echo "BASEPATH: ${BASEPATH}"
echo "Config source: ${CONF_SRC}"

# Create directories
mkdir -p "${BASEPATH}/mysql/conf"
mkdir -p "${BASEPATH}/mysql/data"
mkdir -p "${BASEPATH}/mysql/log"
mkdir -p "${BASEPATH}/seata/conf"
mkdir -p "${BASEPATH}/seata/logs"
mkdir -p "${BASEPATH}/redis/conf"
mkdir -p "${BASEPATH}/redis/data"
mkdir -p "${BASEPATH}/kafka/data"
mkdir -p "${BASEPATH}/elasticsearch/data"
mkdir -p "${BASEPATH}/mongodb/data"
mkdir -p "${BASEPATH}/minio/data"
mkdir -p "${BASEPATH}/nginx/conf/conf.d/sub"
mkdir -p "${BASEPATH}/nginx/html"

# Copy config files if they don't exist
copy_if_not_exists() {
    local src="$1"
    local dest="$2"
    if [ ! -f "${dest}" ]; then
        echo "Copying: ${src} -> ${dest}"
        cp "${src}" "${dest}"
    else
        echo "Exists: ${dest}"
    fi
}

# MySQL config
copy_if_not_exists "${CONF_SRC}/mysql/conf/my.cnf" "${BASEPATH}/mysql/conf/my.cnf"

# Seata config
copy_if_not_exists "${CONF_SRC}/seata/conf/application.yml" "${BASEPATH}/seata/conf/application.yml"

# Redis config
copy_if_not_exists "${CONF_SRC}/redis/conf/redis.conf" "${BASEPATH}/redis/conf/redis.conf"

# Nginx config
NGINX_SRC="${SCRIPT_DIR}/nginx"
copy_if_not_exists "${NGINX_SRC}/conf/nginx.conf" "${BASEPATH}/nginx/conf/nginx.conf"
copy_if_not_exists "${NGINX_SRC}/conf/conf.d/default.conf" "${BASEPATH}/nginx/conf/conf.d/default.conf"
copy_if_not_exists "${NGINX_SRC}/conf/conf.d/sub/ec-demo.conf" "${BASEPATH}/nginx/conf/conf.d/sub/ec-demo.conf"

# Nginx html
copy_if_not_exists "${NGINX_SRC}/html/index.html" "${BASEPATH}/nginx/html/index.html"
copy_if_not_exists "${NGINX_SRC}/html/50x.html" "${BASEPATH}/nginx/html/50x.html"

echo ""
echo "Setup complete!"
echo ""
echo "To start the environment:"
echo "  export BASEPATH=${BASEPATH}"
echo "  docker compose -f docker-compose-demo-env.yml up -d"
