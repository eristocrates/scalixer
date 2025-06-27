#!/bin/bash
set -e

echo "🔧 Setting up Scala and SBT..."

# Set up directories
TOOLS_DIR="$HOME/.tools"
mkdir -p "$TOOLS_DIR/bin"

# Download and install SBT
echo "📦 Installing SBT..."
curl -fsSL https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz -o sbt.tgz
tar -xzf sbt.tgz
mv sbt "$TOOLS_DIR/sbt"
rm sbt.tgz

# Add sbt to PATH
export PATH="$TOOLS_DIR/sbt/bin:$PATH"

# Optionally verify sbt installation
sbt sbtVersion

# Download and install Scala CLI (recommended for modern workflows)
echo "📦 Installing Scala CLI..."
curl -fsSL https://github.com/VirtusLab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-linux.gz -o scala-cli.gz
gunzip scala-cli.gz
chmod +x scala-cli
mv scala-cli "$TOOLS_DIR/bin/scala-cli"

# Add Scala CLI to PATH
export PATH="$TOOLS_DIR/bin:$PATH"

# Verify Scala CLI
scala-cli version

echo "✅ Setup complete. PATH updated and tools installed."

