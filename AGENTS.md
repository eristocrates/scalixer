# AGENTS.md

## setup

```bash
# Install SDKMAN and use it to install Java and SBT
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
sdk install sbt

# Verify installation
java -version
sbt --version
