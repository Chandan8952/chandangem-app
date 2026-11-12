#!/bin/bash
# ============================================================
# ChandanGem ← AntiGem Upstream Sync
# ओरिजिनल AntiGem के नए tools/features/updates को clone में merge करता है
# Branding (icon, app name, package, colors) preserve रहता है.
# Usage: bash sync-upstream.sh
# ============================================================
set -e

CLONE_DIR="/data/user/0/com.termux/files/home/projects/codenexus-app"
UPSTREAM_DIR="/data/user/0/com.termux/files/home/projects/antigem_repo"
UPSTREAM_REMOTE="upstream"   # Chandan8952/antigem (fork of original)
# For true original, add remote: git remote add original https://github.com/santoshkurmi/antigem.git

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  CodeNexus ← AntiGem Upstream Sync"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. Pull latest upstream
echo ""
echo "▶ Step 1: Pulling latest upstream AntiGem..."
cd "$UPSTREAM_DIR"
git fetch "$UPSTREAM_REMOTE" 2>/dev/null || git pull origin master
git pull origin master 2>/dev/null || true
UPSTREAM_COMMIT=$(git log --oneline -1)
echo "   Upstream: $UPSTREAM_COMMIT"

# 2. Save branding values (to restore after sync)
echo ""
echo "▶ Step 2: Saving branding (icon, name, package, colors)..."
cd "$CLONE_DIR"
cp app/build.gradle.kts /tmp/.build.gradle.kts.bak 2>/dev/null || cp app/build.gradle.kts "$CLONE_DIR/.build.gradle.kts.bak"
cp app/src/main/AndroidManifest.xml "$CLONE_DIR/.AndroidManifest.xml.bak"
cp app/src/main/res/values/strings.xml "$CLONE_DIR/.strings.xml.bak" 2>/dev/null || true
# icon files backup
mkdir -p "$CLONE_DIR/.branding_backup"
cp -r app/src/main/res/mipmap-anydpi-v26 "$CLONE_DIR/.branding_backup/" 2>/dev/null || true
cp app/src/main/res/drawable/ic_launcher_foreground.xml "$CLONE_DIR/.branding_backup/" 2>/dev/null || true
cp app/src/main/res/drawable/ic_launcher_background.xml "$CLONE_DIR/.branding_backup/" 2>/dev/null || true

# 3. Sync all source files (excluding branding)
echo ""
echo "▶ Step 3: Syncing source files (tools, features, UI)..."
rsync -av --delete \
  --exclude='*.bak' \
  --exclude='ic_launcher*' \
  --exclude='ic_termux_launcher*' \
  --exclude='strings.xml' \
  --exclude='colors.xml' \
  --exclude='AndroidManifest.xml' \
  --exclude='build.gradle.kts' \
  "$UPSTREAM_DIR/app/src/main/" "$CLONE_DIR/app/src/main/"

# 4. Restore branding
echo ""
echo "▶ Step 4: Restoring branding (icon, name, package stay yours)..."
cp "$CLONE_DIR/.build.gradle.kts.bak" "$CLONE_DIR/app/build.gradle.kts"
cp "$CLONE_DIR/.AndroidManifest.xml.bak" "$CLONE_DIR/app/src/main/AndroidManifest.xml"
cp "$CLONE_DIR/.strings.xml.bak" "$CLONE_DIR/app/src/main/res/values/strings.xml" 2>/dev/null || true
cp -r "$CLONE_DIR/.branding_backup/mipmap-anydpi-v26/." "$CLONE_DIR/app/src/main/res/mipmap-anydpi-v26/" 2>/dev/null || true
cp "$CLONE_DIR/.branding_backup/ic_launcher_foreground.xml" "$CLONE_DIR/app/src/main/res/drawable/" 2>/dev/null || true
cp "$CLONE_DIR/.branding_backup/ic_launcher_background.xml" "$CLONE_DIR/app/src/main/res/drawable/" 2>/dev/null || true

# 5. Commit
echo ""
echo "▶ Step 5: Committing..."
cd "$CLONE_DIR"
git add -A
git commit -m "sync: upstream AntiGem update [$UPSTREAM_COMMIT] (branding preserved)" 2>/dev/null || echo "Nothing new to commit."

# cleanup
rm -f "$CLONE_DIR/.build.gradle.kts.bak" "$CLONE_DIR/.AndroidManifest.xml.bak" "$CLONE_DIR/.strings.xml.bak"
rm -rf "$CLONE_DIR/.branding_backup"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Sync complete! New tools/features propagated; icon & branding preserved."
echo "   Push to trigger new APK build:"
echo "   cd $CLONE_DIR && git push origin master"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"