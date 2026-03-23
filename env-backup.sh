#!/bin/bash
#
# .env ファイルのバックアップ/復元スクリプト
# 使用方法:
#   ./env-backup.sh backup   - .envファイルをバックアップ
#   ./env-backup.sh restore  - .envファイルを復元
#

set -e

# パス設定（スクリプトの場所を基準に相対パス）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
BASEPATH="${BASEPATH:-/Users/wangjw/Dev/_Env/_demo/seata-mode}"
BACKUP_DIR="${BASEPATH}/nginx/apps-env/ec-demo"

# バックアップ対象ファイル（相対パス）
ENV_FILES=(
    ".env"
    "platform/docker/demo/.env"
    "apps/web/.env"
    "apps/web/.env.production"
    "apps/web/.env.development"
)

backup() {
    echo "=== .env ファイルをバックアップ ==="
    mkdir -p "$BACKUP_DIR"

    for env_file in "${ENV_FILES[@]}"; do
        src="$PROJECT_DIR/$env_file"
        # ディレクトリ構造を保持してバックアップ
        dest_dir="$BACKUP_DIR/$(dirname "$env_file")"
        dest="$BACKUP_DIR/$env_file"

        if [[ -f "$src" ]]; then
            mkdir -p "$dest_dir"
            cp "$src" "$dest"
            echo "  ✓ $env_file"
        else
            echo "  - $env_file (存在しない)"
        fi
    done

    echo ""
    echo "バックアップ完了: $BACKUP_DIR"
}

restore() {
    echo "=== .env ファイルを復元 ==="

    if [[ ! -d "$BACKUP_DIR" ]]; then
        echo "エラー: バックアップディレクトリが存在しません: $BACKUP_DIR"
        exit 1
    fi

    for env_file in "${ENV_FILES[@]}"; do
        src="$BACKUP_DIR/$env_file"
        dest="$PROJECT_DIR/$env_file"
        dest_dir="$(dirname "$dest")"

        if [[ -f "$src" ]]; then
            mkdir -p "$dest_dir"
            cp "$src" "$dest"
            echo "  ✓ $env_file"
        else
            echo "  - $env_file (バックアップなし)"
        fi
    done

    echo ""
    echo "復元完了"
}

list() {
    echo "=== 現在の.envファイル状況 ==="
    echo ""
    echo "[プロジェクト: $PROJECT_DIR]"
    for env_file in "${ENV_FILES[@]}"; do
        src="$PROJECT_DIR/$env_file"
        if [[ -f "$src" ]]; then
            echo "  ✓ $env_file"
        else
            echo "  - $env_file (なし)"
        fi
    done
    echo ""
    echo "[バックアップ: $BACKUP_DIR]"
    if [[ -d "$BACKUP_DIR" ]]; then
        for env_file in "${ENV_FILES[@]}"; do
            src="$BACKUP_DIR/$env_file"
            if [[ -f "$src" ]]; then
                echo "  ✓ $env_file"
            else
                echo "  - $env_file (なし)"
            fi
        done
    else
        echo "  (バックアップディレクトリなし)"
    fi
}

case "${1:-}" in
    backup)
        backup
        ;;
    restore)
        restore
        ;;
    list)
        list
        ;;
    *)
        echo "使用方法: $0 {backup|restore|list}"
        echo ""
        echo "  backup  - プロジェクトの.envファイルをバックアップ"
        echo "  restore - バックアップから.envファイルを復元"
        echo "  list    - 現在の.envファイル状況を表示"
        exit 1
        ;;
esac
