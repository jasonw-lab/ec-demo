#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RUN_DIR="${RUN_DIR:-$SCRIPT_DIR/.run}"

SERVICES=(
  bff
  alert-service
  order-service
  es-service
  payment-service
  storage-service
  account-service
)

usage() {
  cat <<'EOF'
Usage:
  ./stop-services.sh [service ...]

Services:
  bff
  order-service
  account-service
  storage-service
  payment-service
  alert-service
  es-service
EOF
}

is_running() {
  local pid="$1"
  [[ -n "$pid" ]] || return 1
  kill -0 "$pid" >/dev/null 2>&1
}

stop_service() {
  local service="$1"
  local pid_file="$RUN_DIR/$service.pid"

  if [[ ! -f "$pid_file" ]]; then
    echo "$service is not running (no pid file)."
    return
  fi

  local pid
  pid="$(cat "$pid_file")"
  if ! is_running "$pid"; then
    echo "$service is not running (stale pid $pid)."
    rm -f "$pid_file"
    return
  fi

  echo "Stopping $service (pid $pid)"
  kill "$pid"

  for _ in {1..30}; do
    if ! is_running "$pid"; then
      rm -f "$pid_file"
      echo "$service stopped."
      return
    fi
    sleep 1
  done

  echo "$service did not stop within 30 seconds; sending SIGKILL."
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pid_file"
  echo "$service stopped."
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

REQUESTED=()
if [[ $# -eq 0 || "${1:-}" == "all" ]]; then
  REQUESTED=("${SERVICES[@]}")
else
  for service in "$@"; do
    case "$service" in
      bff|order-service|account-service|storage-service|payment-service|alert-service|es-service)
        REQUESTED+=("$service")
        ;;
      *)
        echo "Unknown service: $service" >&2
        usage >&2
        exit 1
        ;;
    esac
  done
fi

for service in "${REQUESTED[@]}"; do
  stop_service "$service"
done
