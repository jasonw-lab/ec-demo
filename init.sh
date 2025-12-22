#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# init.sh
# プロジェクト内の指定された環境ファイルを
# /Dev/_Env/app 以下にプロジェクトと同じパス構造でコピーするスクリプト

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
DEST_BASE="/Users/wangjw/Dev/_Env/app"

FILES=(
  "bff/src/main/resources/serviceAccountKey.json"
  "front/.env"
  "front/.env.developemnt"
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
  echo "Pushing environment files to $DEST_BASE (preserving path structure)..."
  for rel in "${FILES[@]}"; do
    src="$REPO_ROOT/$rel"
    dest_dir="$DEST_BASE/$(dirname "$rel")"
    if [ ! -e "$src" ]; then
      echo "Warning: source not found: $src" >&2
      continue
    fi
    mkdir -p "$dest_dir"
    cp -p "$src" "$dest_dir/"
    echo "Copied: $src -> $dest_dir/"
  done
  echo "Push done."
elif [ "$action" = "pull" ]; then
  echo "Pulling environment files from $DEST_BASE -> project (skipping existing files)..."
  for rel in "${FILES[@]}"; do
    src="$DEST_BASE/$rel"
    dest_dir="$REPO_ROOT/$(dirname "$rel")"
    dest_file="$dest_dir/$(basename "$rel")"
    if [ ! -e "$src" ]; then
      echo "Warning: source not found in DEST_BASE: $src" >&2
      continue
    fi
    if [ -e "$dest_file" ]; then
      echo "Skip (exists): $dest_file"
      continue
    fi
    mkdir -p "$dest_dir"
    cp -p "$src" "$dest_dir/"
    echo "Pulled: $src -> $dest_dir/"
  done
  echo "Pull done."
else
  usage
  exit 2
fi
