# Backlog - Toluja Print Agent v2

Este backlog organiza a construcao da v2 do agente local de impressao. A prioridade
inicial e entregar uma instalacao Windows simples para usuario leigo, com Java embutido
e configuracao pronta por cliente.

## Marco 1 - Fundacao do projeto Java

- [x] Criar estrutura `app/` com Gradle.
- [x] Definir Java 21 como versao alvo.
- [x] Configurar build de JAR executavel.
- [x] Criar classe principal `TolujaPrintAgent`.
- [x] Criar comando `--version`.
- [x] Criar comando `--config-check`.
- [x] Adicionar logging simples em arquivo e console.
- [x] Definir convencao de versao do agente.
- [x] Criar `samples/config.example.json`.

## Marco 2 - Configuracao local

- [x] Definir modelo final do `config.json`.
- [x] Implementar `ConfigLoader`.
- [x] Validar campos obrigatorios:
  - `apiBaseUrl`
  - `tenantId`
  - `storeId`
  - `deviceId`
  - `printKey`
  - `pollIntervalMs`
- [x] Validar lista de impressoras configuradas.
- [x] Mascarar `printKey` em logs.
- [x] Suportar caminho de config por argumento `--config`.
- [x] Suportar caminho padrao Windows em `C:\ProgramData\Toluja\PrintAgent\config.json`.
- [x] Suportar caminho padrao Linux em `/etc/toluja/print-agent/config.json`.

## Marco 3 - Cliente da API Toluja

- [x] Implementar `PrintAgentClient`.
- [x] Implementar `GET /api/print-agent/jobs/next?deviceId=...`.
- [x] Enviar header `X-Print-Key`.
- [x] Tratar `204 No Content`.
- [x] Tratar erros HTTP com mensagens claras.
- [x] Implementar parsing do `NextJobResponse`.
- [x] Implementar `POST /api/print-agent/jobs/{jobId}/ack`.
- [x] Implementar DTOs:
  - `NextJobResponse`
  - `JobDelivery`
  - `AckRequest`
  - `DeliveryAck`
  - `AckResponse`
- [x] Adicionar timeout HTTP configuravel.
- [x] Adicionar retry/backoff para falha temporaria de rede.

## Marco 4 - Loop de execucao

- [x] Implementar `JobPoller`.
- [x] Rodar polling com `pollIntervalMs`.
- [x] Evitar processamento concorrente do mesmo job.
- [x] Decodificar `payloadBase64`.
- [x] Validar `payloadType`.
- [x] Processar todas as `deliveries`.
- [x] Gerar ACK por delivery.
- [x] Garantir ACK mesmo quando uma delivery falha.
- [x] Registrar ultimo job processado no log.
- [x] Controlar parada limpa do processo.

## Marco 5 - Impressao MVP

- [x] Criar interface `PrintBackend`.
- [x] Criar `PrintDispatcher`.
- [x] Implementar canal `WINDOWS_QUEUE`.
- [x] Implementar canal `CUPS`.
- [x] Rejeitar canal desconhecido com erro claro.
- [x] Validar `copies >= 1`.
- [x] Implementar timeout por impressao.
- [x] Implementar impressao de texto simples.
- [x] Criar comando local `--test-print`.
- [x] Criar comando local `--list-printers`.
- [x] Detectar impressoras locais quando possivel.
- [x] Definir fallback manual caso a deteccao nao encontre a impressora correta.

## Marco 6 - Windows MVP

- [x] Escolher instalador: Inno Setup ou WiX.
- [x] Baixar WinSW em versao fixa no pacote de build.
- [x] Criar template de servico WinSW.
- [x] Criar layout de instalacao:
  - app em `C:\Program Files\Toluja\PrintAgent`
  - config/logs em `C:\ProgramData\Toluja\PrintAgent`
- [x] Gerar runtime Java reduzido com `jlink`.
- [x] Implementar definicao do instalador `.exe`.
- [ ] Gerar instalador `.exe` em ambiente Windows com Inno Setup.
- [x] Instalar servico `TolujaPrintAgent`.
- [x] Configurar start automatico.
- [x] Configurar restart automatico em falha.
- [x] Criar desinstalador.
- [ ] Testar instalacao em Windows limpo.
- [ ] Testar reinstalacao por cima.
- [ ] Testar remocao completa.

## Marco 7 - Pacote por cliente

- [x] Definir entrada para geracao de pacote:
  - `apiBaseUrl`
  - `tenantId`
  - `storeId`
  - `deviceId`
  - `printKey`
  - impressoras
- [x] Criar script local para gerar pacote Windows por cliente.
- [x] Incluir `config.json` pre-preenchido no instalador.
- [x] Nomear pacote como `TolujaPrintAgent-Cliente-Loja-Setup.exe`.
- [x] Garantir que o usuario final nao precise editar arquivo.
- [x] Documentar processo interno para suporte gerar instalador.
- [ ] Implementar tela no backend para gerar pacote por cliente/loja.
- [x] Garantir regra de um agente por loja.
- [x] Definir como a impressora detectada sera vinculada ao pacote/configuracao.

## Marco 8 - Diagnostico

- [x] Implementar comando `--status`.
- [x] Mostrar versao instalada.
- [x] Mostrar caminho da config carregada.
- [x] Mostrar API configurada.
- [x] Mostrar deviceId/storeId.
- [x] Testar conectividade com backend.
- [x] Listar impressoras detectadas.
- [x] Mostrar ultimo job recebido.
- [x] Mostrar ultimo erro de impressao.
- [x] Criar comando `--diagnostics-zip`.
- [x] Incluir logs no ZIP.
- [x] Incluir config mascarada no ZIP.
- [x] Incluir informacoes do sistema operacional.

## Marco 9 - Linux MVP

- [x] Criar pacote `.tar.gz`.
- [x] Criar script `install.sh`.
- [x] Instalar app em `/opt/toluja/print-agent`.
- [x] Instalar config em `/etc/toluja/print-agent/config.json`.
- [x] Criar unit systemd `toluja-print-agent.service`.
- [x] Configurar restart automatico.
- [x] Verificar presenca do comando `lp`.
- [x] Documentar instalacao Ubuntu/Debian.
- [ ] Testar com CUPS real.
- [ ] Testar em Raspberry, se continuar sendo alvo.

## Marco 10 - Qualidade e testes

- [x] Gerar e versionar Gradle Wrapper.
- [x] Substituir parser JSON interno por Gson apos Gradle Wrapper estar disponivel.
- [x] Testar `ConfigLoader`.
- [x] Testar parsing dos DTOs.
- [x] Testar cliente HTTP com servidor fake.
- [x] Testar ACK de sucesso.
- [x] Testar ACK com erro parcial.
- [x] Testar canal desconhecido.
- [x] Testar `copies` invalido.
- [x] Testar mascara de segredo nos logs.
- [x] Testar geracao de ZIP de diagnostico.
- [x] Criar teste manual documentado para Windows.
- [x] Criar teste manual documentado para Linux.

## Marco 11 - Backend futuro

- [ ] Adicionar endpoint de heartbeat.
- [ ] Adicionar status do agente no painel.
- [ ] Adicionar endpoint de teste remoto de impressao.
- [ ] Adicionar endpoint de configuracao remota.
- [ ] Adicionar controle de versao minima.
- [ ] Adicionar registro de ultimo erro por dispositivo.
- [ ] Adicionar revogacao de `printKey`.
- [ ] Adicionar geracao de pacote por cliente no painel administrativo.
- [ ] Adicionar fluxo de selecao de impressora detectada, se a deteccao for feita pelo agente.

## Marco 12 - Melhorias futuras de impressao

- [ ] Avaliar impressao RAW/ESC-POS no Windows.
- [ ] Avaliar biblioteca ESC/POS Java.
- [ ] Suportar payload `RAW`.
- [ ] Suportar payload `PDF`.
- [ ] Suportar payload `IMAGE`.
- [ ] Permitir corte de papel, abertura de gaveta e beep.
- [ ] Mapear impressoras por ID logico em vez de nome local.
- [ ] Permitir reimpressao controlada pelo backend.

## Riscos conhecidos

- [ ] Validar se Java + `jlink` fica com tamanho aceitavel para download.
- [ ] Validar se WinSW funciona bem em Windows 10/11 comuns dos clientes.
- [ ] Validar se antivirus bloqueia instalador nao assinado.
- [ ] Decidir quando comprar certificado de assinatura de codigo depois do MVP.
- [ ] Validar nomes reais de filas Windows em impressoras termicas comuns.
- [ ] Validar se `Out-Printer` atende ou se sera necessario RAW/ESC-POS.
- [ ] Definir estrategia quando o cliente troca o nome da impressora.

## Definicao de pronto do MVP

- [ ] Instalador Windows `.exe` instala sem comandos manuais.
- [ ] Agente inicia automaticamente apos instalacao.
- [ ] Agente reinicia sozinho apos reboot.
- [ ] Configuracao vem pronta no pacote do cliente.
- [ ] Agente busca job no backend atual.
- [ ] Agente trabalha somente com jobs online, sem fila offline local.
- [ ] Agente imprime em pelo menos uma fila Windows.
- [ ] Agente envia ACK de sucesso/erro.
- [x] Logs permitem diagnosticar falha basica.
- [ ] Desinstalador remove servico e arquivos instalados.
- [ ] Documentacao interna explica como gerar pacote para um cliente.
