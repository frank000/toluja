#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-dev}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${REPO_ROOT}"

COMMIT="none"
if command -v git >/dev/null 2>&1 && git rev-parse --short HEAD >/dev/null 2>&1; then
  COMMIT="$(git rev-parse --short HEAD)"
fi

export VERSION
export GIT_COMMIT="${COMMIT}"

GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "-s -w -X main.version=${VERSION} -X main.commit=${GIT_COMMIT} -X main.buildTime=$(date -u +%Y-%m-%dT%H:%M:%SZ)" -o dist/print-agent-windows-amd64.exe .

PACKAGE_DIR="dist/windows-release"
mkdir -p "${PACKAGE_DIR}"
cp dist/print-agent-windows-amd64.exe "${PACKAGE_DIR}/"
cp .env.example "${PACKAGE_DIR}/"
cp windows/install-windows.ps1 windows/uninstall-windows.ps1 "${PACKAGE_DIR}/"

echo "Pacote Windows pronto em ${PACKAGE_DIR}"
