#!/usr/bin/env bash
set -euo pipefail

INSTALL_DIR="/opt/toluja/print-agent"
CONFIG_DIR="/etc/toluja/print-agent"
SERVICE_NAME="toluja-print-agent"
FORCE_CONFIG="false"

usage() {
  cat <<USAGE
Uso: sudo ./install.sh [opcoes]

Opcoes:
  --install-dir DIR    Diretorio do app (default: /opt/toluja/print-agent)
  --config-dir DIR     Diretorio do config (default: /etc/toluja/print-agent)
  --service-name NAME  Nome do servico systemd (default: toluja-print-agent)
  --force-config       Sobrescreve config.json existente
  -h, --help           Mostra esta ajuda
USAGE
}

while (($# > 0)); do
  case "$1" in
    --install-dir)
      INSTALL_DIR="${2:-}"
      shift 2
      ;;
    --config-dir)
      CONFIG_DIR="${2:-}"
      shift 2
      ;;
    --service-name)
      SERVICE_NAME="${2:-}"
      shift 2
      ;;
    --force-config)
      FORCE_CONFIG="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Opcao invalida: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Execute como root: sudo ./install.sh" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ ! -f "${SCRIPT_DIR}/app/toluja-print-agent.jar" ]]; then
  echo "Pacote invalido: app/toluja-print-agent.jar nao encontrado." >&2
  exit 1
fi
if [[ ! -x "${SCRIPT_DIR}/runtime/bin/java" ]]; then
  echo "Pacote invalido: runtime/bin/java nao encontrado ou sem permissao." >&2
  exit 1
fi
if [[ ! -f "${SCRIPT_DIR}/config/config.json" ]]; then
  echo "Pacote invalido: config/config.json nao encontrado." >&2
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "systemctl nao encontrado. Este instalador exige Linux com systemd." >&2
  exit 1
fi

if ! command -v lp >/dev/null 2>&1; then
  echo "Aviso: comando 'lp' nao encontrado. Instale CUPS para usar canal CUPS." >&2
fi

mkdir -p "${INSTALL_DIR}/app" "${INSTALL_DIR}/runtime" "${CONFIG_DIR}"
cp -f "${SCRIPT_DIR}/app/toluja-print-agent.jar" "${INSTALL_DIR}/app/toluja-print-agent.jar"
rm -rf "${INSTALL_DIR}/runtime"
cp -R "${SCRIPT_DIR}/runtime" "${INSTALL_DIR}/runtime"

TARGET_CONFIG="${CONFIG_DIR}/config.json"
if [[ "${FORCE_CONFIG}" == "true" || ! -f "${TARGET_CONFIG}" ]]; then
  install -m 0640 "${SCRIPT_DIR}/config/config.json" "${TARGET_CONFIG}"
fi

UNIT_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
sed \
  -e "s|/opt/toluja/print-agent|${INSTALL_DIR}|g" \
  -e "s|/etc/toluja/print-agent|${CONFIG_DIR}|g" \
  "${SCRIPT_DIR}/systemd/toluja-print-agent.service" > "${UNIT_FILE}"

systemctl daemon-reload
systemctl enable --now "${SERVICE_NAME}.service"

echo "Instalacao concluida."
echo "App: ${INSTALL_DIR}"
echo "Config: ${TARGET_CONFIG}"
echo "Servico: ${SERVICE_NAME}.service"
echo "Status: systemctl status ${SERVICE_NAME}.service --no-pager"
echo "Logs: journalctl -u ${SERVICE_NAME}.service -f"
