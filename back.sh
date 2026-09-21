#!/bin/bash

# Backend deployment script that selectively rebuilds services based on args.
set -euo pipefail

# Set default for iTerm2 variable if not set (prevents unbound variable error)
export ITERM2_SQUELCH_MARK="${ITERM2_SQUELCH_MARK:-0}"

# BASEPATH設定（デフォルト: /mydata）

source "$HOME/.bashrc"
export BASEPATH="${BASEPATH:-/mydata}"
ENV_FILE="${BASEPATH}/nginx/apps-env/ec-demo/platform/docker/demo/.env"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [[ $# -eq 0 ]]; then
  echo "No backend services specified. Skipping deployment."
  exit 0
fi

# git pull is handled by deploy.yml when called from GitHub Actions
# Uncomment below for standalone execution
# echo "Updating repository..."
# git pull

declare -a services=()

add_service() {
  local svc="$1"
  for existing in "${services[@]:-}"; do
    if [[ "$existing" == "$svc" ]]; then
      return
    fi
  done
  services+=("$svc")
}

map_argument_to_service() {
  case "$1" in
    account-service)
      add_service "ec-demo-account-service"
      ;;
    order-service)
      add_service "ec-demo-order-service"
      ;;
    storage-service)
      add_service "ec-demo-storage-service"
      ;;
    payment-service)
      add_service "ec-demo-payment-service"
      ;;
    es-service)
      add_service "ec-demo-es-service"
      ;;
    bff)
      add_service "ec-demo-bff"
      ;;
    *)
      echo "Warning: Unknown service identifier '$1' ignored."
      ;;
  esac
}

for svc in "$@"; do
  map_argument_to_service "$svc"
done

if [[ ${#services[@]} -eq 0 ]]; then
  echo "No recognized backend services requested. Exiting."
  exit 0
fi

echo "Services to rebuild: ${services[*]}"
echo "BASEPATH: $BASEPATH"

cd "$SCRIPT_DIR/platform/docker"
COMPOSE_FILE="demo/docker-compose-demo-app.yml"

# env-fileが存在する場合は使用
if [[ -f "$ENV_FILE" ]]; then
  echo "Using env-file: $ENV_FILE"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build "${services[@]}"
else
  echo "Warning: env-file not found: $ENV_FILE"
  docker compose -f "$COMPOSE_FILE" up -d --build "${services[@]}"
fi

echo "Selected backend services redeployed successfully."