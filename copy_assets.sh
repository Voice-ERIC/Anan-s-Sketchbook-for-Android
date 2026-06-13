#!/bin/bash
# 复制 BaseImages 和字体到 Android assets 目录
# 从项目根目录运行：bash android/copy_assets.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$SCRIPT_DIR/app/src/main/assets"

echo "复制 BaseImages..."
cp -r "$PROJECT_DIR/BaseImages" "$ASSETS_DIR/"
echo "复制字体..."
cp "$PROJECT_DIR/font.ttf" "$ASSETS_DIR/fonts/"
echo "完成!"
echo ""
echo "assets 目录内容:"
find "$ASSETS_DIR" -type f | sort
