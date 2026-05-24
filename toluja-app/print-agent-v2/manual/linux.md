# Teste Manual - Linux

## Pre-requisitos

- Linux com systemd.
- JDK 21 na maquina de build.
- CUPS instalado para validar canal `CUPS`.

## Geracao do pacote

```bash
cd print-agent-v2
./packaging/linux/scripts/build-linux-package.sh
```

## Instalacao

```bash
tar -xzf build/linux-package/dist/toluja-print-agent-linux-0.1.0-dev.tar.gz
cd toluja-print-agent-linux-0.1.0-dev
sudo ./install.sh
```

## Validacoes

```bash
systemctl status toluja-print-agent.service --no-pager
journalctl -u toluja-print-agent.service -n 100 --no-pager
/opt/toluja/print-agent/runtime/bin/java \
  -jar /opt/toluja/print-agent/app/toluja-print-agent.jar \
  --config /etc/toluja/print-agent/config.json \
  --config-check
```

## Diagnostico

```bash
/opt/toluja/print-agent/runtime/bin/java \
  -jar /opt/toluja/print-agent/app/toluja-print-agent.jar \
  --config /etc/toluja/print-agent/config.json \
  --status

/opt/toluja/print-agent/runtime/bin/java \
  -jar /opt/toluja/print-agent/app/toluja-print-agent.jar \
  --config /etc/toluja/print-agent/config.json \
  --diagnostics-zip /tmp/toluja-print-agent-diagnostics.zip
```

Enviar o arquivo `/tmp/toluja-print-agent-diagnostics.zip` para o suporte quando houver falha.

## CUPS

```bash
command -v lp
lpstat -a
lp -d NOME_DA_FILA /etc/hosts
```

## Teste de impressao

```bash
/opt/toluja/print-agent/runtime/bin/java \
  -jar /opt/toluja/print-agent/app/toluja-print-agent.jar \
  --config /etc/toluja/print-agent/config.json \
  --test-print balcao
```

## Remocao manual

```bash
sudo systemctl disable --now toluja-print-agent.service
sudo rm -f /etc/systemd/system/toluja-print-agent.service
sudo systemctl daemon-reload
sudo rm -rf /opt/toluja/print-agent
sudo rm -rf /etc/toluja/print-agent
```
