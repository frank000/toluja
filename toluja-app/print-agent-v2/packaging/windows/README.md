# Windows Packaging

Empacotamento Windows da v2 do Toluja Print Agent.

## Decisao do Marco 6

O instalador escolhido para o MVP Windows e **Inno Setup**.

Motivos:

- gera `.exe` simples para usuario final;
- exige menos infraestrutura que WiX;
- suporta instalacao com administrador;
- permite copiar app/runtime/config e executar comandos de servico;
- atende melhor ao objetivo inicial de instalacao simples.

O servico Windows e gerenciado por **WinSW**.

## Layout instalado

App:

```text
C:\Program Files\Toluja\PrintAgent
  app\toluja-print-agent.jar
  runtime\bin\javaw.exe
  TolujaPrintAgentService.exe
  TolujaPrintAgentService.xml
```

Config/logs:

```text
C:\ProgramData\Toluja\PrintAgent
  config.json
  logs\
```

## Gerar pacote por cliente

Exemplo:

```powershell
cd print-agent-v2
powershell -ExecutionPolicy Bypass -File .\packaging\windows\scripts\build-client-package.ps1 `
  -ManifestPath .\samples\customer-package\customer-package.example.json
```

O manifest contem `clientSlug`, `storeSlug`, `agentVersion` e o objeto `config`.
O `config` e gravado como `config.json` no instalador.

## Saida esperada

```text
build\windows-package\installer\TolujaPrintAgent-Cliente-Loja-Setup.exe
```

## Dependencias para gerar instalador completo

- Windows 10/11 ou Windows Server;
- JDK 21;
- PowerShell;
- Inno Setup com `ISCC.exe` no `PATH`;
- acesso a internet para baixar WinSW, ou arquivo ja presente em `build\windows-package\downloads`.

Para montar apenas o staging sem Inno Setup:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\scripts\build-client-package.ps1 `
  -ManifestPath .\samples\customer-package\customer-package.example.json `
  -SkipInstaller
```

## Fluxo para suporte

1. Gerar ou copiar a print key do cliente no backend.
2. Confirmar `tenantId`, `storeId` e `deviceId`.
3. Detectar ou confirmar nome da impressora local.
4. Criar manifest do cliente.
5. Rodar `build-client-package.ps1`.
6. Enviar o `.exe` gerado para o cliente.

O usuario final nao deve editar `config.json`.
