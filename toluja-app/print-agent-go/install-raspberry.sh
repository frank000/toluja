#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="toluja-print-agent"
INSTALL_DIR="/opt/toluja/print-agent"
TARGET_BIN_NAME="toluja-print-agent"
SOURCE_BIN=""
ENV_SOURCE=""
FORCE_ENV="false"

usage() {
  cat <<USAGE
Uso: sudo ./install-raspberry.sh [opcoes]

Opcoes:
  --source-bin PATH   Caminho do binario a instalar.
                      Default: detecta em ./dist/ pela arquitetura da maquina.
  --env-file PATH     Caminho do arquivo .env para copiar.
                      Default: usa ./.env se existir; senao cria a partir de .env.example.
  --force-env         Sobrescreve o .env no diretorio de instalacao.
  --install-dir DIR   Diretorio de instalacao (default: /opt/toluja/print-agent)
  --service-name NAME Nome do servico systemd (default: toluja-print-agent)
  -h, --help          Mostra esta ajuda.
USAGE
}

while (($# > 0)); do
  case "$1" in
    --source-bin)
      SOURCE_BIN="${2:-}"
      shift 2
      ;;
    --env-file)
      ENV_SOURCE="${2:-}"
      shift 2
      ;;
    --force-env)
      FORCE_ENV="true"
      shift
      ;;
    --install-dir)
      INSTALL_DIR="${2:-}"
      shift 2
      ;;
    --service-name)
      SERVICE_NAME="${2:-}"
      shift 2
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
  echo "Execute como root (ex.: sudo ./install-raspberry.sh)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

pick_default_binary() {
  local arch
  arch="$(uname -m)"

  case "${arch}" in
    aarch64|arm64)
      echo "${SCRIPT_DIR}/dist/print-agent-linux-arm64"
      ;;
    armv7l|armv7)
      echo "${SCRIPT_DIR}/dist/print-agent-linux-armv7"
      ;;
    *)
      echo "Arquitetura nao suportada automaticamente: ${arch}" >&2
      echo "Informe --source-bin manualmente." >&2
      exit 1
      ;;
  esac
}

if [[ -z "${SOURCE_BIN}" ]]; then
  SOURCE_BIN="$(pick_default_binary)"
fi

if [[ ! -f "${SOURCE_BIN}" ]]; then
  echo "Binario nao encontrado: ${SOURCE_BIN}" >&2
  echo "Gere antes com: ./build.sh" >&2
  exit 1
fi

if [[ -z "${ENV_SOURCE}" ]]; then
  if [[ -f "${SCRIPT_DIR}/.env" ]]; then
    ENV_SOURCE="${SCRIPT_DIR}/.env"
  elif [[ -f "${SCRIPT_DIR}/.env.example" ]]; then
    ENV_SOURCE="${SCRIPT_DIR}/.env.example"
  else
    echo "Nao encontrei .env nem .env.example no projeto." >&2
    exit 1
  fi
fi

if [[ ! -f "${ENV_SOURCE}" ]]; then
  echo "Arquivo de ambiente nao encontrado: ${ENV_SOURCE}" >&2
  exit 1
fi

mkdir -p "${INSTALL_DIR}"
install -m 0755 "${SOURCE_BIN}" "${INSTALL_DIR}/${TARGET_BIN_NAME}"

TARGET_ENV="${INSTALL_DIR}/.env"
if [[ "${FORCE_ENV}" == "true" || ! -f "${TARGET_ENV}" ]]; then
  install -m 0640 "${ENV_SOURCE}" "${TARGET_ENV}"
fi

UNIT_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
cat > "${UNIT_FILE}" <<UNIT
[Unit]
Description=Toluja Print Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=${INSTALL_DIR}
EnvironmentFile=${INSTALL_DIR}/.env
ExecStart=${INSTALL_DIR}/${TARGET_BIN_NAME}
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable --now "${SERVICE_NAME}.service"

if ! command -v lp >/dev/null 2>&1; then
  echo "Aviso: comando 'lp' nao encontrado. Instale CUPS para usar canal CUPS." >&2
fi

echo "Instalacao concluida."
echo "Servico: ${SERVICE_NAME}.service"
echo "Binario: ${INSTALL_DIR}/${TARGET_BIN_NAME}"
echo "Env: ${TARGET_ENV}"
echo "Status: systemctl status ${SERVICE_NAME}.service --no-pager"
echo "Logs: journalctl -u ${SERVICE_NAME}.service -f"
