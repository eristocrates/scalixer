#!/usr/bin/env bash
set -euo pipefail

# --- JAVA SETUP ---
# Set JAVA_HOME and add java.exe to PATH
export JAVA_HOME="$PWD/tools/jdk-17.0.15.6-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# Confirm Java is available
if ! command -v java >/dev/null 2>&1; then
  echo "❌ Java not found at $JAVA_HOME"
  exit 1
fi

# --- SBT SETUP ---
# Point to SBT launcher JAR and create a wrapper script
SBT_DIR="$PWD/tools/sbt-launch"
mkdir -p "$SBT_DIR/bin"

cat > "$SBT_DIR/bin/sbt" <<'EOF'
#!/usr/bin/env bash
java -Xms512M -Xmx1G -jar "$(dirname "$0")/sbt-launch.jar" "$@"
EOF

chmod +x "$SBT_DIR/bin/sbt"

# Add to PATH
export PATH="$SBT_DIR/bin:$PATH"

# Confirm SBT works
if ! command -v sbt >/dev/null 2>&1; then
  echo "❌ SBT setup failed"
  exit 1
fi

echo "✅ Environment configured with offline Java and SBT."
