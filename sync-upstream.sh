#!/bin/bash
# ============================================================
# ChandanGem Upstream Sync Script
# ओरिजिनल AntiGem ऐप के नए अपडेट्स को क्लोन में मर्ज करें
# Usage: bash sync-upstream.sh
# ============================================================

set -e

CLONE_DIR="/data/user/0/com.termux/files/home/projects/chandangem-app"
UPSTREAM_DIR="/data/user/0/com.termux/files/home/projects/antigem_repo"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ChandanGem ← AntiGem Upstream Sync"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. AntiGem repo से latest code कॉपी करें
echo ""
echo "▶ Step 1: Syncing latest AntiGem code..."
cd "$UPSTREAM_DIR"
git pull origin master

# 2. नई Kotlin फ़ाइलें chandangem-app में कॉपी करें
# (package name रखते हुए)
echo ""
echo "▶ Step 2: Copying updated source files..."
SOURCE="$UPSTREAM_DIR/app/src/main/java/com/example/gemini"
DEST="$CLONE_DIR/app/src/main/java/com/chandankumar/chandangem"

# सिर्फ़ core logic files sync करें (UI और package-specific छोड़ें)
rsync -av --include="*/" --include="*.kt" --exclude="*" \
  --exclude="MainActivity.kt" \
  "$SOURCE/data/" "$DEST/core/upstream/" 2>/dev/null || true

echo ""
echo "▶ Step 3: Committing changes..."
cd "$CLONE_DIR"
git add -A
UPSTREAM_COMMIT=$(cd "$UPSTREAM_DIR" && git log --oneline -1)
git commit -m "sync: upstream AntiGem update [$UPSTREAM_COMMIT]" 2>/dev/null || echo "Nothing new to commit."

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Sync complete! Now push to trigger new APK build:"
echo "   cd $CLONE_DIR && git push origin master"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
