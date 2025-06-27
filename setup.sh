#!/bin/bash
set -e

echo "Setting up Scala 3 environment..."

# Ensure Java is installed
if ! type -p java > /dev/null; then
  echo "Java is required. Please install OpenJDK 17+."
  exit 1
fi

# Install sbt if not available
if ! type -p sbt > /dev/null; then
  echo "Installing sbt..."
  curl -L https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz | tar xz
  export PATH="$PWD/sbt/bin:$PATH"
fi

# Compile once to fetch dependencies
sbt compile
