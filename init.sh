#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# init.sh
# プロジェクト内の指定された環境ファイルを
# /Dev/_Env/app 以下にプロジェクトと同じパス構造でコピーするスクリプト
#
# 使い方（コマンド実行サンプル）
#
# 1) 実行権限を付与してデフォルト（push）を実行（プロジェクト -> DEST_BASE、上書き）
#    chmod +x ./init.sh
#    ./init.sh
#
# 2) 明示的に push を実行（上書き）
#    ./init.sh push
#
# 3) pull を実行して DEST_BASE からプロジェクトへコピー（プロジェクト側にファイルがあればスキップ）
#    ./init.sh pull
#
# 例: CI/CD の前に環境ファイルをローカルから集約する場合は push を使い、
#     ローカルに環境ファイルが無ければチーム共通の DEST_BASE から pull で配布します。

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
DEST_BASE="/Users/wangjw/Dev/_Env/app-key"
APP_NAME="ec-demo"

FILES=(
  "bff/src/main/resources/serviceAccountKey.json"
  "front/.env"
  "front/.env.development"
  ".env"
  ".idea/runConfigurations"
)

usage() {
  cat <<EOF
Usage: $0 [push|pull]

  push  (default) : copy from project -> $DEST_BASE (overwrite)
  pull            : copy from $DEST_BASE -> project (skip if target exists)
EOF
}

action="${1:-push}"

if [ "$action" = "push" ]; then
  echo "Pushing environment files to $DEST_BASE/$APP_NAME (preserving path structure)..."
  for rel in "${FILES[@]}"; do
    src="$REPO_ROOT/$rel"
    dest_dir="$DEST_BASE/$APP_NAME/$(dirname "$rel")"
    dest_path="$dest_dir/$(basename "$rel")"
    if [ ! -e "$src" ]; then
      echo "Warning: source not found: $src" >&2
      continue
    fi
    mkdir -p "$dest_dir"
    if [ -e "$dest_path" ]; then
      rm -rf "$dest_path"
    fi
    cp -rp "$src" "$dest_dir/"
    echo "Copied: $src -> $dest_dir/"
  done
  echo "Push done."
elif [ "$action" = "pull" ]; then
  echo "Pulling environment files from $DEST_BASE/$APP_NAME -> project (skipping existing files)..."
  for rel in "${FILES[@]}"; do
    src="$DEST_BASE/$APP_NAME/$rel"
    dest_dir="$REPO_ROOT/$(dirname "$rel")"
    dest_path="$dest_dir/$(basename "$rel")"
    if [ ! -e "$src" ]; then
      echo "Warning: source not found in DEST_BASE: $src" >&2
      continue
    fi
    if [ -e "$dest_path" ]; then
      echo "File/directory exists: $dest_path"
      read -r -p "Overwrite? Type 'yes' to overwrite: " answer
      if [ "$answer" != "yes" ]; then
        echo "Skip (user chose not to overwrite): $dest_path"
        continue
      fi
      rm -rf "$dest_path"
    fi
    mkdir -p "$dest_dir"
    cp -rp "$src" "$dest_dir/"
    echo "Pulled: $src -> $dest_dir/"
  done
  echo "Pull done."
else
  usage
  exit 2
fi
