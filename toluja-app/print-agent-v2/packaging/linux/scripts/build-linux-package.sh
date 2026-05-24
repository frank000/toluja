#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${VERSION:-0.1.0-dev}"
CONFIG_PATH="${CONFIG_PATH:-${PROJECT_ROOT}/samples/config.example.json}"
OUT_DIR="${OUT_DIR:-${PROJECT_ROOT}/build/linux-package}"
PACKAGE_NAME="${PACKAGE_NAME:-toluja-print-agent-linux-${VERSION}}"

usage() {
  cat <<USAGE
Uso: ./packaging/linux/scripts/build-linux-package.sh [opcoes]

Opcoes via ambiente:
  VERSION       Versao do agente (default: 0.1.0-dev)
  CONFIG_PATH   Config a incluir no pacote (default: samples/config.example.json)
  OUT_DIR       Diretorio de saida (default: build/linux-package)
  PACKAGE_NAME  Nome base do pacote
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ ! -f "${CONFIG_PATH}" ]]; then
  echo "Config nao encontrado: ${CONFIG_PATH}" >&2
  exit 1
fi

if ! command -v jlink >/dev/null 2>&1; then
  echo "jlink nao encontrado. Instale JDK 21." >&2
  exit 1
fi

STAGE_DIR="${OUT_DIR}/stage/${PACKAGE_NAME}"
rm -rf "${STAGE_DIR}"
mkdir -p "${STAGE_DIR}/app" "${STAGE_DIR}/config" "${STAGE_DIR}/systemd"

echo "==> Compilando JAR"
if [[ ! -x "${PROJECT_ROOT}/gradlew" ]]; then
  echo "Gradle Wrapper nao encontrado: ${PROJECT_ROOT}/gradlew" >&2
  exit 1
fi
"${PROJECT_ROOT}/gradlew" -p "${PROJECT_ROOT}" :app:jar "-PagentVersion=${VERSION}"
JAR_PATH="$(find "${PROJECT_ROOT}/app/build/libs" -maxdepth 1 -name 'toluja-print-agent*.jar' | sort | tail -n 1)"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "JAR nao encontrado apos build." >&2
  exit 1
fi
cp "${JAR_PATH}" "${STAGE_DIR}/app/toluja-print-agent.jar"

echo "==> Gerando runtime jlink"
jlink \
  --add-modules java.base,java.desktop,java.logging,java.net.http \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=zip-6 \
  --output "${STAGE_DIR}/runtime"

echo "==> Copiando config, install.sh e systemd"
cp "${CONFIG_PATH}" "${STAGE_DIR}/config/config.json"
cp "${PROJECT_ROOT}/packaging/linux/package/install.sh" "${STAGE_DIR}/install.sh"
chmod +x "${STAGE_DIR}/install.sh"
cp "${PROJECT_ROOT}/packaging/linux/systemd/toluja-print-agent.service" "${STAGE_DIR}/systemd/toluja-print-agent.service"

mkdir -p "${OUT_DIR}/dist"
TAR_PATH="${OUT_DIR}/dist/${PACKAGE_NAME}.tar.gz"
tar -C "${OUT_DIR}/stage" -czf "${TAR_PATH}" "${PACKAGE_NAME}"

echo "Pacote Linux gerado: ${TAR_PATH}"
