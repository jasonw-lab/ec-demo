#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env}"
RUN_DIR="${RUN_DIR:-$SCRIPT_DIR/.run}"
LOG_DIR="${LOG_DIR:-$RUN_DIR/logs}"
SKIP_BUILD="${SKIP_BUILD:-false}"

mkdir -p "$RUN_DIR" "$LOG_DIR"

declare -A MODULES=(
  [bff]="apps/bff"
  [order-service]="apps/services/order-service"
  [account-service]="apps/services/account-service"
  [storage-service]="apps/services/storage-service"
  [payment-service]="apps/services/payment-service"
  [alert-service]="apps/services/alert-service"
  [es-service]="apps/services/es-service"
)

declare -A PORTS=(
  [bff]="8080"
  [order-service]="8082"
  [account-service]="8081"
  [storage-service]="8083"
  [payment-service]="8084"
  [alert-service]="8085"
  [es-service]="8086"
)

SERVICES=(
  account-service
  storage-service
  payment-service
  es-service
  order-service
  alert-service
  bff
)

usage() {
  cat <<'EOF'
Usage:
  ./start-services.sh [service ...]

Services:
  bff
  order-service
  account-service
  storage-service
  payment-service
  alert-service
  es-service

Environment:
  ENV_FILE=/path/to/.env   Defaults to repository root .env
  RUN_DIR=/path/to/.run    Defaults to repository root .run
  MAVEN_CMD=/path/to/mvn   Overrides Maven command
  SKIP_BUILD=true          Start existing target/*.jar files without building

The ENV_FILE is loaded as shell environment variables before services start.
EOF
}

is_running() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] || return 1

  local pid
  pid="$(cat "$pid_file")"
  [[ -n "$pid" ]] || return 1
  kill -0 "$pid" >/dev/null 2>&1
}

find_jar() {
  local module="$1"
  find "$module/target" \
    -maxdepth 1 \
    -type f \
    -name "*.jar" \
    ! -name "*-sources.jar" \
    ! -name "*-javadoc.jar" \
    ! -name "*.original" \
    | sort \
    | tail -n 1
}

detect_maven() {
  if [[ -n "${MAVEN_CMD:-}" ]]; then
    echo "$MAVEN_CMD"
    return
  fi

  if [[ -x "$SCRIPT_DIR/mvnw" ]]; then
    echo "$SCRIPT_DIR/mvnw"
    return
  fi

  if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return
  fi
}

load_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Env file not found: $ENV_FILE" >&2
    exit 1
  fi

  echo "Loading environment variables from $ENV_FILE"
  local tmp_env
  tmp_env="$(mktemp)"
  # Keep only valid shell variable assignments (skip lines like 'server.port=8080'
  # that are valid Spring properties but not valid bash identifiers).
  grep -E '^[[:space:]]*[A-Za-z_][A-Za-z0-9_]*=' "$ENV_FILE" >"$tmp_env" || true
  set -a
  # shellcheck disable=SC1090
  source "$tmp_env"
  set +a
  rm -f "$tmp_env"
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
    if [[ -z "${MODULES[$service]:-}" ]]; then
      echo "Unknown service: $service" >&2
      usage >&2
      exit 1
    fi
    REQUESTED+=("$service")
  done
fi

load_env

BUILD_MODULES=()
for service in "${REQUESTED[@]}"; do
  pid_file="$RUN_DIR/$service.pid"
  if is_running "$pid_file"; then
    echo "$service is already running (pid $(cat "$pid_file"))."
    continue
  fi
  rm -f "$pid_file"
  BUILD_MODULES+=("${MODULES[$service]}")
done

if [[ ${#BUILD_MODULES[@]} -eq 0 ]]; then
  echo "No services to start."
  exit 0
fi

if [[ "$SKIP_BUILD" != "true" ]]; then
  MAVEN_BIN="$(detect_maven || true)"
  if [[ -z "$MAVEN_BIN" ]]; then
    missing_jars=()
    for service in "${REQUESTED[@]}"; do
      pid_file="$RUN_DIR/$service.pid"
      if is_running "$pid_file"; then
        continue
      fi
      if [[ -z "$(find_jar "${MODULES[$service]}")" ]]; then
        missing_jars+=("$service")
      fi
    done

    if [[ ${#missing_jars[@]} -gt 0 ]]; then
      echo "Maven command not found and jar files are missing for: ${missing_jars[*]}" >&2
      echo "Install Maven, set MAVEN_CMD=/path/to/mvn, add Maven Wrapper, or build once elsewhere." >&2
      exit 1
    fi

    echo "Maven command not found; using existing target/*.jar files."
  else
    MODULE_LIST="$(IFS=,; echo "${BUILD_MODULES[*]}")"
    echo "Building modules: $MODULE_LIST"
    "$MAVEN_BIN" -pl "$MODULE_LIST" -am -DskipTests package
  fi
else
  echo "SKIP_BUILD=true; using existing target/*.jar files."
fi

for service in "${REQUESTED[@]}"; do
  pid_file="$RUN_DIR/$service.pid"
  if is_running "$pid_file"; then
    continue
  fi

  module="${MODULES[$service]}"
  jar_file="$(find_jar "$module")"
  if [[ -z "$jar_file" ]]; then
    echo "Jar not found for $service under $module/target" >&2
    exit 1
  fi

  log_file="$LOG_DIR/$service.log"
  port="${PORTS[$service]}"

  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
  elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java.exe" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java.exe"
  elif command -v java >/dev/null 2>&1; then
    JAVA_BIN="java"
  else
    echo "java executable not found (set JAVA_HOME or add java to PATH)" >&2
    exit 1
  fi

  echo "Starting $service on port $port"
  nohup "$JAVA_BIN" -jar "$jar_file" \
    "--spring.config.import=optional:file:$ENV_FILE[.properties]" \
    "--server.port=$port" \
    >"$log_file" 2>&1 &

  pid="$!"
  echo "$pid" >"$pid_file"
  echo "$service started (pid $pid, log $log_file)"
done
