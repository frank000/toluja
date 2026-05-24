# Toluja Print Agent v2 - Planejamento

Este diretorio nasce para planejar uma versao 2 totalmente nova do agente de impressao
local do Toluja. A prioridade da v2 e reduzir atrito de instalacao para usuarios leigos,
principalmente no Windows, mantendo suporte Linux como segunda plataforma.

## Objetivo

Criar um agente local que:

- seja instalado no Windows com poucos cliques;
- venha pre-configurado para o cliente/loja que contratou o Toluja GO;
- rode em segundo plano e reinicie sozinho;
- imprima em filas locais do Windows e Linux/CUPS;
- tenha diagnostico claro quando algo falhar;
- seja simples de atualizar e desinstalar;
- nao exija que o cliente edite `.env`, rode comandos ou instale dependencias manualmente.

## Estado atual da implementacao

Os marcos 1 a 7, 9 e 10 do backlog ja possuem implementacao inicial:

- projeto Java 21 com Gradle;
- JAR executavel configurado;
- classe principal `TolujaPrintAgent`;
- comandos `--version`, `--help`, `--config-check`, `--once`, `--list-printers` e `--test-print`;
- logging simples em console e arquivo;
- `ConfigLoader` para `config.json`;
- validacao dos campos obrigatorios;
- mascara de `printKey`;
- caminhos padrao de configuracao para Windows e Linux;
- exemplo em `samples/config.example.json`.
- cliente HTTP para o contrato atual do print-agent;
- loop de polling com `pollIntervalMs`;
- processamento de jobs `TEXT` e envio de ACK.
- dispatcher de impressao por canal;
- canal `WINDOWS_QUEUE` via fila local do Windows;
- canal `CUPS` via comando `lp`;
- deteccao local de impressoras quando possivel.
- empacotamento Windows com Inno Setup e WinSW;
- script local para gerar pacote por cliente/loja com `config.json` pronto.
- empacotamento Linux `.tar.gz` com runtime `jlink`, `install.sh` e systemd;
- Gradle Wrapper versionado;
- Gson no lugar do parser JSON interno;
- testes automatizados com JUnit.

Execucao direta em desenvolvimento:

```bash
cd print-agent-v2
./gradlew :app:jar
java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --config-check
java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --list-printers
java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --test-print balcao
java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --once
```

Testes automatizados:

```bash
cd print-agent-v2
./gradlew test
```

Gerar pacote Windows por cliente:

```powershell
cd print-agent-v2
powershell -ExecutionPolicy Bypass -File .\packaging\windows\scripts\build-client-package.ps1 `
  -ManifestPath .\samples\customer-package\customer-package.example.json
```

Gerar pacote Linux:

```bash
cd print-agent-v2
./packaging/linux/scripts/build-linux-package.sh
```

Manuais de teste:

```text
manual/windows.md
manual/linux.md
```

## Investigacao da versao atual

A versao atual em `print-agent-go` ja resolve o fluxo basico:

- le configuracao por `.env` ou `config.json`;
- consulta o backend em `GET /api/print-agent/jobs/next?deviceId=...`;
- usa `X-Print-Key` para autenticar;
- recebe `payloadBase64` e uma lista de `deliveries`;
- imprime em:
  - Linux: canal `CUPS`, usando o comando `lp`;
  - Windows: canal `WINDOWS_QUEUE`, usando PowerShell `Out-Printer`;
- envia ACK em `POST /api/print-agent/jobs/{jobId}/ack`;
- possui scripts de instalacao Windows e Linux/Raspberry;
- possui pacote one-click para Windows com configuracao pre-preenchida.

Principais limitacoes percebidas para usuario final Windows:

- instalacao ainda depende de scripts PowerShell/BAT e permissao de administrador;
- antivirus, ExecutionPolicy, UAC e bloqueio de arquivos baixados podem atrapalhar;
- falhas ficam pouco visiveis para usuario leigo;
- nao ha tela simples de status, teste de impressora ou diagnostico;
- configuracao por `.env`/`config.json` e tecnica demais;
- atualizacao e suporte remoto ainda dependem de acao manual;
- `Out-Printer` e simples, mas pouco controlavel para impressoras termicas/ESC/POS;
- nao existe um pacote Windows "produto", como instalador assinado com wizard.

## Recomendacao de stack

### Escolha recomendada: Java 21 + runtime embutido

Mesmo sendo mais pesado que Go em tamanho de pacote, Java pode facilitar muito a vida
no Windows quando combinado com um instalador correto.

Proposta:

- Java 21 LTS;
- aplicacao CLI/servico, sem framework pesado;
- HTTP client nativo do Java (`java.net.http`);
- JSON com Jackson ou Moshi/Gson;
- runtime reduzido com `jlink`, sem exigir Java instalado no computador do cliente;
- empacotamento Windows com Inno Setup ou WiX Toolset;
- servico Windows via WinSW;
- Linux com pacote `.tar.gz` e script systemd; depois `.deb` se necessario.

Resultado esperado:

- cliente baixa `Instalador Toluja Print Agent.exe`;
- abre o instalador;
- confirma permissao de administrador;
- o instalador copia app + runtime + configuracao;
- registra o servico;
- inicia automaticamente;
- mostra uma tela final com "instalado" e orientacao minima.

### Por que nao depender de Java instalado?

Porque o publico alvo e leigo. Pedir para instalar JRE/JDK aumenta muito o suporte.
A v2 deve levar seu proprio runtime Java reduzido dentro do instalador.

### Alternativas avaliadas

- Go v2: continuaria muito leve, mas o problema atual no Windows parece mais de
  produto/instalador/diagnostico do que da linguagem.
- Rust: gera binarios leves, mas aumenta a complexidade de desenvolvimento e integracao
  com impressao Windows.
- .NET: bom para Windows, mas Linux e runtime self-contained tambem ficariam maiores.
- Java Native Image/GraalVM: pode virar executavel nativo leve, mas adiciona complexidade
  de build. Pode ser avaliado depois, nao para o MVP.

## Arquitetura proposta

```text
print-agent-v2/
  README.md
  app/
    src/main/java/...
    build.gradle.kts
  packaging/
    windows/
      installer/
      winsw/
    linux/
      systemd/
  samples/
    config.example.json
```

Componentes internos:

- `ConfigLoader`: carrega configuracao instalada pelo pacote do cliente;
- `PrintAgentClient`: conversa com a API do Toluja;
- `JobPoller`: busca jobs com backoff e controle de concorrencia;
- `PrintDispatcher`: decide qual backend de impressao usar;
- `WindowsPrintBackend`: imprime em fila Windows;
- `CupsPrintBackend`: imprime via CUPS no Linux;
- `EscPosRenderer` ou `RawPrintBackend`: etapa futura para impressora termica;
- `AckReporter`: envia resultado de cada delivery;
- `HealthReporter`: envia status do agente para backend, se o backend suportar;
- `Diagnostics`: gera logs e um pacote simples para suporte.

## Contrato atual com backend

A v2 deve manter compatibilidade inicial com o contrato existente:

- `GET /api/print-agent/jobs/next?deviceId={deviceId}`
- Header: `X-Print-Key: <tenant_print_key>`
- `204 No Content` quando nao ha job;
- `200 OK` com:
  - `jobId`
  - `tenantId`
  - `storeId`
  - `deviceId`
  - `orderId`
  - `payloadType`
  - `payloadBase64`
  - `createdAt`
  - `deliveries`
- `POST /api/print-agent/jobs/{jobId}/ack`
- Status de delivery: `SUCCESS` ou `ERROR`.

Para v2, recomenda-se evoluir o backend depois com endpoints opcionais:

- `POST /api/print-agent/devices/heartbeat`
- `POST /api/print-agent/devices/diagnostics`
- `GET /api/print-agent/devices/{deviceId}/config`
- `POST /api/print-agent/jobs/{jobId}/progress`

Esses endpoints nao devem bloquear o MVP.

## Configuracao por cliente

A v2 deve ser distribuida por cliente/loja ja com configuracao embutida no pacote.

Exemplo de `config.json` instalado:

```json
{
  "apiBaseUrl": "https://app.toluja.com.br",
  "tenantId": "cliente-x",
  "storeId": "loja-001",
  "deviceId": "agent-loja-001",
  "printKey": "chave-gerada-pelo-backend",
  "pollIntervalMs": 1000,
  "httpTimeoutMs": 20000,
  "apiRetryAttempts": 3,
  "apiRetryBackoffMs": 500,
  "printTimeoutMs": 30000,
  "printers": [
    {
      "id": "balcao",
      "name": "Balcao",
      "channel": "WINDOWS_QUEUE",
      "destination": "EPSON TM-T20X Receipt"
    },
    {
      "id": "cozinha",
      "name": "Cozinha",
      "channel": "WINDOWS_QUEUE",
      "destination": "BEMATECH MP-4200 TH"
    }
  ]
}
```

O instalador do cliente nao deve pedir esses dados. Quem gera o pacote e o Toluja.

## Instalacao Windows

Formato recomendado para MVP:

- `TolujaPrintAgent-ClienteX-Loja001-Setup.exe`;
- criado com Inno Setup ou WiX;
- instala em `C:\Program Files\Toluja\PrintAgent`;
- copia runtime Java reduzido para dentro da pasta do app;
- copia `config.json` pre-preenchido;
- registra servico `TolujaPrintAgent` com WinSW;
- configura restart automatico;
- cria atalhos opcionais:
  - "Status do Toluja Print Agent";
  - "Enviar diagnostico para suporte";
  - "Desinstalar Toluja Print Agent".

Servico Windows:

- usar WinSW para rodar `javaw.exe -jar toluja-print-agent.jar`;
- logs em `C:\ProgramData\Toluja\PrintAgent\logs`;
- configuracao em `C:\ProgramData\Toluja\PrintAgent\config.json`;
- app instalado em `C:\Program Files\Toluja\PrintAgent`.

Observacao: `ProgramData` e melhor para configuracao/logs porque nao depende de editar
arquivos dentro de `Program Files`.

## Instalacao Linux

Formato recomendado para MVP:

- pacote `.tar.gz` por cliente;
- script `install.sh`;
- instala em `/opt/toluja/print-agent`;
- configuracao em `/etc/toluja/print-agent/config.json`;
- logs via `journalctl`;
- cria `toluja-print-agent.service`.

Depois do MVP:

- gerar `.deb` para Ubuntu/Debian;
- manter `.tar.gz` para distros diversas e Raspberry.

## Impressao

MVP:

- Windows: imprimir em fila local pelo nome da impressora;
- Linux: imprimir via CUPS usando `lp`;
- payload inicial: `TEXT`, mantendo compatibilidade com backend atual.

Melhorias planejadas:

- listar impressoras locais no diagnostico;
- comando de teste de impressao;
- timeout por delivery;
- retries controlados;
- suporte a conteudo RAW/ESC-POS para impressoras termicas;
- suporte a PDF/imagem se o Toluja passar a gerar payloads desse tipo;
- mapeamento remoto de impressoras pelo backend, evitando configurar nomes manualmente.

## Diagnostico e suporte

A v2 deve ter diagnostico simples. Para o usuario leigo, isso e mais importante que logs
tecnicamente completos.

Minimo para MVP:

- arquivo de log rotativo;
- comando/tela "Verificar instalacao";
- mostrar:
  - servico rodando ou parado;
  - API acessivel ou nao;
  - deviceId configurado;
  - impressoras detectadas;
  - ultimo job recebido;
  - ultimo erro de impressao;
  - versao instalada;
- gerar ZIP de diagnostico para suporte.

Implementado no Marco 8:

- comando `--status`;
- comando `--diagnostics-zip [PATH]`;
- persistencia local de ultimo job, ultimo ACK e ultimo erro em `state.json`;
- ZIP com status, sistema operacional, impressoras, config mascarada, estado e logs.

Ideal depois:

- heartbeat no painel Toluja;
- botao no backend para "testar impressora";
- aviso quando o agente fica offline;
- atualizacao remota assistida.

## Seguranca

- `printKey` deve ficar em arquivo local com permissao restrita;
- nunca registrar `printKey` completo nos logs;
- instalador gerado por cliente deve ter validade/revogacao via backend;
- usar HTTPS em producao;
- validar tamanho maximo de payload;
- validar canais aceitos;
- impedir path traversal ou execucao de comandos vindos do backend;
- logs devem mascarar segredos.

## Atualizacao

MVP:

- novo instalador sobrescreve a versao anterior;
- mantem `config.json` existente, salvo quando pacote for explicitamente gerado para trocar config;
- servico e reiniciado ao final.

Depois:

- endpoint de versao disponivel;
- aviso no painel;
- auto-update com assinatura de pacote, apenas se realmente necessario.

## Roadmap sugerido

### Fase 1 - MVP funcional

- criar projeto Java minimalista;
- implementar contrato atual da API;
- carregar `config.json`;
- imprimir em Windows Queue e CUPS;
- gerar ACK;
- logs basicos;
- build local.

### Fase 2 - Instalador Windows de verdade

- criar runtime reduzido com `jlink`;
- empacotar com Inno Setup ou WiX;
- registrar servico com WinSW;
- instalar config em `ProgramData`;
- criar pacote por cliente;
- testar em Windows limpo.

### Fase 3 - Diagnostico

- comando `status`;
- comando `test-print`;
- ZIP de diagnostico;
- listagem de impressoras;
- mensagens de erro amigaveis.

### Fase 4 - Linux

- script systemd;
- pacote `.tar.gz`;
- teste com CUPS;
- documentacao Ubuntu/Debian/Raspberry.

### Fase 5 - Evolucao backend

- heartbeat;
- status no painel;
- teste remoto de impressao;
- configuracao remota;
- telemetria minima de versao/saude.

## Decisoes iniciais

1. A v2 sera planejada em Java 21, com runtime embutido no instalador.
2. O cliente nao devera instalar Java manualmente.
3. O Windows sera a plataforma principal.
4. O Linux sera suportado por systemd/CUPS.
5. O contrato atual com o backend sera mantido no MVP.
6. O instalador sera gerado por cliente/loja com configuracao pronta.
7. Diagnostico e instalacao simples sao requisitos de produto, nao detalhes secundarios.
8. O backend tera uma tela para gerar pacote por cliente/loja.
9. Cada loja tera apenas um computador/agente.
10. O MVP imprimira texto simples; ESC/POS fica para evolucao posterior.
11. A deteccao automatica de impressoras e preferida quando for tecnicamente possivel.
12. Os jobs sempre virao online; nao e necessario manter fila offline local no MVP.
13. O pacote Windows nao precisa ser assinado digitalmente desde o inicio.
14. A configuracao nao sera atualizada pelo backend sem reinstalacao no momento.
15. O parser JSON interno foi substituido por Gson no Marco 10.
16. O pacote gerado por cliente/loja nao precisa expirar.

## Perguntas respondidas

- O backend tera uma tela para gerar pacote por cliente/loja.
- O cliente nao tera mais de um computador/agente por loja.
- O Toluja precisa imprimir texto simples por enquanto; ESC/POS sera planejado mais a frente.
- Os nomes das impressoras devem ser detectados pelo agente, se possivel.
- O agente apenas imprimira jobs recebidos online.
- O pacote Windows nao precisa ser assinado digitalmente desde o inicio.

## Perguntas em aberto

- Como sera o fluxo no backend para selecionar a impressora detectada e gerar o pacote?
- A deteccao de impressoras acontecera antes da instalacao, durante a instalacao ou depois pelo agente?
- O suporte precisa conseguir trocar a impressora vinculada sem reinstalar o agente?
