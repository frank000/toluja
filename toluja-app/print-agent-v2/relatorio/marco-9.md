# Relatorio - Marco 9

## Pontos resolvidos

- Criado empacotamento Linux em `packaging/linux`.
- Criado script `build-linux-package.sh`.
- Criado pacote `.tar.gz` com:
  - `app/toluja-print-agent.jar`
  - runtime Java gerado por `jlink`
  - `config/config.json`
  - `systemd/toluja-print-agent.service`
  - `install.sh`
- Criado script `install.sh`.
- Instalacao do app definida em `/opt/toluja/print-agent`.
- Configuracao definida em `/etc/toluja/print-agent/config.json`.
- Criada unit systemd `toluja-print-agent.service`.
- Configurado restart automatico com `Restart=always`.
- Instalador verifica presenca do comando `lp` e avisa quando CUPS nao esta disponivel.
- Documentada instalacao Ubuntu/Debian em `packaging/linux/README.md`.
- Status posterior ao Marco 8: manual Linux atualizado com `--status` e `--diagnostics-zip`.

## Dificuldades encontradas

- O pacote Linux usa runtime `jlink` da arquitetura onde o build roda.
- Raspberry precisa de build no proprio Raspberry ou ambiente Linux ARM compativel.
- Nao foi feito teste com CUPS real/fila fisica neste ambiente.

## Validacao executada

- Pacote `.tar.gz` gerado com sucesso.
- Scripts Linux validados com `bash -n`.
- Pacote extraido localmente.
- JAR dentro do pacote executado com `--config-check`.

## Pontos de melhoria

- Testar com CUPS real.
- Testar em Raspberry se ele continuar como alvo.
- Avaliar geracao futura de pacote `.deb`.
- Adicionar script de desinstalacao Linux, se necessario.
- Avaliar diretorio dedicado em `/var/lib/toluja/print-agent` para `state.json` em uma evolucao futura.
