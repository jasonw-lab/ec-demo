#!/bin/bash

# Import用のテストデータをセットアップするスクリプト
# Docker環境で /tmp/ec-demo-import にCSVと画像を配置

set -e

IMPORT_DIR="${EC_DEMO_IMPORT_DIR:-/tmp/ec-demo-import}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "Import データセットアップ"
echo "=========================================="
echo "Import Dir: ${IMPORT_DIR}"
echo ""

# ディレクトリ作成
echo "📁 ディレクトリを作成..."
mkdir -p "${IMPORT_DIR}/images"

# CSVファイルをコピー
echo "📄 CSVファイルをコピー..."
if [ -f "${SCRIPT_DIR}/sample-products.csv" ]; then
  cp "${SCRIPT_DIR}/sample-products.csv" "${IMPORT_DIR}/"
  echo "✅ sample-products.csv をコピーしました"
else
  echo "⚠️  sample-products.csv が見つかりません"
fi

# ダミー画像を生成（ImageMagick使用、なければスキップ）
echo ""
echo "🖼️  ダミー画像を生成..."
if command -v convert &> /dev/null; then
  for id in {1001..1015}; do
    if [ ! -f "${IMPORT_DIR}/images/${id}.jpg" ]; then
      convert -size 800x600 xc:lightblue \
        -pointsize 48 -fill black -gravity center \
        -annotate +0+0 "Product ${id}" \
        "${IMPORT_DIR}/images/${id}.jpg"
    fi
  done
  echo "✅ ダミー画像を生成しました（1001.jpg - 1015.jpg）"
else
  echo "⚠️  ImageMagickが見つかりません。画像は手動で配置してください。"
  echo "   画像ファイル名: 1001.jpg, 1002.jpg, ..., 1015.jpg"
fi

# 結果表示
echo ""
echo "=========================================="
echo "セットアップ完了"
echo "=========================================="
echo "CSV: ${IMPORT_DIR}/sample-products.csv"
echo "Images: ${IMPORT_DIR}/images/*.jpg"
echo ""
echo "次のコマンドでImportを実行:"
echo "  cd ${SCRIPT_DIR}"
echo "  ./load-products.sh"
