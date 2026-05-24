# Teste Manual - Windows

## Pre-requisitos

- Windows 10/11.
- Uma fila de impressora local instalada.
- JDK 21 e Inno Setup apenas na maquina que gera o instalador.

## Geracao do pacote

```powershell
cd print-agent-v2
powershell -ExecutionPolicy Bypass -File .\packaging\windows\scripts\build-client-package.ps1 `
  -ManifestPath .\samples\customer-package\customer-package.example.json
```

## Instalacao

1. Copiar o `.exe` gerado para a maquina de teste.
2. Executar como administrador.
3. Confirmar que o servico `TolujaPrintAgent` foi criado.
4. Confirmar que o servico iniciou automaticamente.

## Validacoes

```powershell
Get-Service TolujaPrintAgent
Get-Content "C:\ProgramData\Toluja\PrintAgent\config.json"
& "C:\Program Files\Toluja\PrintAgent\runtime\bin\java.exe" `
  -jar "C:\Program Files\Toluja\PrintAgent\app\toluja-print-agent.jar" `
  --list-printers
```

## Diagnostico

```powershell
& "C:\Program Files\Toluja\PrintAgent\runtime\bin\java.exe" `
  -jar "C:\Program Files\Toluja\PrintAgent\app\toluja-print-agent.jar" `
  --config "C:\ProgramData\Toluja\PrintAgent\config.json" `
  --status

& "C:\Program Files\Toluja\PrintAgent\runtime\bin\java.exe" `
  -jar "C:\Program Files\Toluja\PrintAgent\app\toluja-print-agent.jar" `
  --config "C:\ProgramData\Toluja\PrintAgent\config.json" `
  --diagnostics-zip "$env:USERPROFILE\Desktop\toluja-print-agent-diagnostics.zip"
```

Enviar o ZIP gerado na area de trabalho para o suporte quando houver falha.

## Teste de impressao

```powershell
& "C:\Program Files\Toluja\PrintAgent\runtime\bin\java.exe" `
  -jar "C:\Program Files\Toluja\PrintAgent\app\toluja-print-agent.jar" `
  --config "C:\ProgramData\Toluja\PrintAgent\config.json" `
  --test-print balcao
```

## Reinstalacao

1. Executar o mesmo instalador novamente.
2. Confirmar que o servico foi recriado/iniciado.
3. Confirmar que a pasta de config continua em `C:\ProgramData\Toluja\PrintAgent`.

## Desinstalacao

1. Desinstalar pelo painel do Windows.
2. Confirmar que o servico `TolujaPrintAgent` foi removido.
3. Confirmar se arquivos residuais precisam ser removidos manualmente.
