# Linux Packaging

Empacotamento Linux da v2 do Toluja Print Agent.

## Saida

O pacote Linux e um `.tar.gz` com:

```text
toluja-print-agent-linux-<versao>/
  app/toluja-print-agent.jar
  runtime/bin/java
  config/config.json
  systemd/toluja-print-agent.service
  install.sh
```

## Gerar pacote

```bash
cd print-agent-v2
./packaging/linux/scripts/build-linux-package.sh
```

Com config especifico:

```bash
VERSION=0.1.0 CONFIG_PATH=/caminho/config.json ./packaging/linux/scripts/build-linux-package.sh
```

## Instalar

No Linux alvo:

```bash
tar -xzf toluja-print-agent-linux-0.1.0-dev.tar.gz
cd toluja-print-agent-linux-0.1.0-dev
sudo ./install.sh
```

O instalador:

- instala app em `/opt/toluja/print-agent`;
- instala config em `/etc/toluja/print-agent/config.json`;
- cria `toluja-print-agent.service`;
- habilita restart automatico;
- verifica se `lp` existe e avisa se CUPS nao estiver disponivel.

## Comandos uteis

```bash
systemctl status toluja-print-agent.service --no-pager
journalctl -u toluja-print-agent.service -f
sudo systemctl restart toluja-print-agent.service
```

## Ubuntu/Debian

Para usar canal `CUPS`, instale CUPS antes:

```bash
sudo apt-get update
sudo apt-get install -y cups
```

Depois configure/teste a fila local:

```bash
lpstat -a
lp -d NOME_DA_FILA /etc/hosts
```

## Raspberry

O pacote usa o runtime gerado por `jlink` na arquitetura onde o build roda.
Para Raspberry, gere o pacote no proprio Raspberry ou em ambiente Linux ARM compativel.
