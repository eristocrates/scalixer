#!/usr/bin/env bash

# Fail on error
set -e

# Install SDKMAN if not already installed
if [ ! -d "$HOME/.sdkman" ]; then
  echo "Installing SDKMAN..."
  curl -s "https://get.sdkman.io" | bash
fi

# Initialize SDKMAN environment
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 21 (or fallback to 17 if 21 fails)
echo "Installing Java..."
sdk install java 21.0.2-tem || sdk install java 17.0.9-tem

# Set Java version
sdk use java 21.0.2-tem || sdk use java 17.0.9-tem

# Install coursier
echo "Installing coursier"
curl -fL https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz | gzip -d > cs && chmod +x cs && ./cs setup

# Confirm versions
echo "Java version:"
java -version
echo "SBT version:"
sbt --version
