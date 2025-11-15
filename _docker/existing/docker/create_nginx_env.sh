#!/bin/bash

# Create directories for MySQL
mkdir -p /mydata/mysql/conf
mkdir -p /mydata/mysql/data
mkdir -p /mydata/sql/mysql

# Create directories for Nacos
mkdir -p /mydata/nacos/logs

# Create directories for Seata
mkdir -p /mydata/seata/config
mkdir -p /mydata/seata/logs

# Create directories for Redis
mkdir -p /mydata/redis/data
mkdir -p /mydata/redis/config

# Create directories for Minio
mkdir -p /mydata/minio/data
mkdir -p /mydata/minio/config

# Create directories for XXL-Job-Admin
mkdir -p /mydata/xxljob/logs

# Create directories for Nginx
mkdir -p /mydata/nginx/conf
mkdir -p /mydata/nginx/conf/conf.d
mkdir -p /mydata/nginx/html
mkdir -p /mydata/nginx/logs

# Get the script's directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# Copy MySQL configuration files
if [ -d "$REPO_ROOT/backend/docker/mysql/conf" ] && [ "$(ls -A $REPO_ROOT/backend/docker/mysql/conf)" ]; then
    cp -r "$REPO_ROOT/backend/docker/mysql/conf/"* /mydata/mysql/conf/
    echo "MySQL configuration files copied successfully."
else
    echo "MySQL configuration directory is empty or does not exist. Skipping."
fi

# Copy Nginx configuration and html files
if [ -d "$REPO_ROOT/backend/docker/nginx/conf" ] && [ "$(ls -A $REPO_ROOT/backend/docker/nginx/conf)" ]; then
    cp -r "$REPO_ROOT/backend/docker/nginx/conf/"* /mydata/nginx/conf/
    echo "Nginx configuration files copied successfully."
else
    echo "Nginx configuration directory is empty or does not exist. Skipping."
fi

if [ -d "$REPO_ROOT/backend/docker/nginx/html" ] && [ "$(ls -A $REPO_ROOT/backend/docker/nginx/html)" ]; then
    cp -r "$REPO_ROOT/backend/docker/nginx/html/"* /mydata/nginx/html/
    echo "Nginx HTML files copied successfully."
else
    echo "Nginx HTML directory is empty or does not exist. Skipping."
fi

echo "All required directories have been created and files have been copied."
