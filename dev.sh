#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# dev.sh — Start Quarkus dev mode with JBR 21 + Enhanced HotSwap
#
# JetBrains Runtime (JBR) 21 provides DCEVM-style enhanced class redefinition:
#   -XX:+AllowEnhancedClassRedefinition  → add/remove methods & fields at runtime
#   -XX:HotswapAgent=core                → JBR built-in HotswapAgent (CDI-aware)
#
# Usage:
#   ./dev.sh           # starts Quarkus dev mode
#   ./dev.sh --debug   # + remote debug on port 5005
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JBR_HOME="${HOME}/.sdkman/candidates/java/21.0.11-jbr"

if [[ ! -d "$JBR_HOME" ]]; then
  echo "❌  JBR 21 not found at $JBR_HOME"
  echo "    Install with:  sdk install java 21.0.11-jbr"
  exit 1
fi

export JAVA_HOME="$JBR_HOME"
export PATH="${JAVA_HOME}/bin:${PATH}"

echo "✅  Using JDK: $(java -version 2>&1 | head -1)"
echo "📂  Project:  ${SCRIPT_DIR}"
echo ""

EXTRA_JVM_ARGS="-XX:+AllowEnhancedClassRedefinition"

# Pass JVM args to the forked Quarkus child process via MAVEN_OPTS
# and also to the app JVM via quarkus.test.jvm-args / quarkus.dev.jvm-args
export MAVEN_OPTS="${MAVEN_OPTS:-} ${EXTRA_JVM_ARGS}"

exec "${SCRIPT_DIR}/mvnw" quarkus:dev \
  -Djvm.args="${EXTRA_JVM_ARGS}" \
  "$@"
