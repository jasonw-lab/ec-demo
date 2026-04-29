#!/usr/bin/env bash
# =============================================================================
# Upload product images to MinIO
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_SRC_DIR="${IMAGE_SRC_DIR:-${SCRIPT_DIR}/data/product/images}"

MINIO_CONTAINER="${MINIO_CONTAINER:-ec-demo-minio}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-ec-demo}"
MINIO_PATH="${MINIO_PATH:-product/images}"

echo "============================================="
echo "MinIO Product Image Upload"
echo "============================================="
echo "Source:    ${IMAGE_SRC_DIR}"
echo "Container: ${MINIO_CONTAINER}"
echo "Endpoint:  ${MINIO_ENDPOINT}"
echo "Bucket:    ${MINIO_BUCKET}"
echo "Path:      ${MINIO_PATH}"
echo "============================================="

# Check if source directory exists
if [ ! -d "${IMAGE_SRC_DIR}" ]; then
  echo "ERROR: Source directory not found: ${IMAGE_SRC_DIR}"
  exit 1
fi

# Check if minio container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${MINIO_CONTAINER}$"; then
  echo "ERROR: MinIO container '${MINIO_CONTAINER}' is not running"
  exit 1
fi

# Setup mc alias
echo ""
echo "[1/4] Setting up mc alias..."
docker exec "${MINIO_CONTAINER}" mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}"

# Create bucket if not exists
echo ""
echo "[2/4] Creating bucket '${MINIO_BUCKET}'..."
docker exec "${MINIO_CONTAINER}" mc mb "local/${MINIO_BUCKET}" --ignore-existing

# Set bucket policy to public (for image access)
echo ""
echo "[3/4] Setting bucket policy to public..."
docker exec "${MINIO_CONTAINER}" mc anonymous set public "local/${MINIO_BUCKET}"

# Copy images to container and upload
echo ""
echo "[4/4] Uploading images..."
TMP_DIR="/tmp/product-images-$$"
docker exec "${MINIO_CONTAINER}" mkdir -p "${TMP_DIR}"

# Copy images from host to container
docker cp "${IMAGE_SRC_DIR}/." "${MINIO_CONTAINER}:${TMP_DIR}/"

# Upload to MinIO
docker exec "${MINIO_CONTAINER}" mc cp --recursive "${TMP_DIR}/" "local/${MINIO_BUCKET}/${MINIO_PATH}/"

# Cleanup temp directory
docker exec "${MINIO_CONTAINER}" rm -rf "${TMP_DIR}"

# Verify upload
echo ""
echo "============================================="
echo "Upload complete. Verifying..."
echo "============================================="
docker exec "${MINIO_CONTAINER}" mc ls "local/${MINIO_BUCKET}/${MINIO_PATH}/"

echo ""
echo "Access URL: ${MINIO_ENDPOINT}/${MINIO_BUCKET}/${MINIO_PATH}/<filename>"
echo "Example:    ${MINIO_ENDPOINT}/${MINIO_BUCKET}/${MINIO_PATH}/1001.jpg"
