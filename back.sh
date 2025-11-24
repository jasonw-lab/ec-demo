#!/bin/bash

# Backend deployment script that selectively rebuilds services based on args.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [[ $# -eq 0 ]]; then
  echo "No backend services specified. Skipping deployment."
  exit 0
fi

echo "Updating repository..."
git pull

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

cd "$SCRIPT_DIR/_docker"
COMPOSE_FILE="demo/docker-compose-demo-app.yml"

docker compose -f "$COMPOSE_FILE" up -d --build "${services[@]}"

echo "Selected backend services redeployed successfully."