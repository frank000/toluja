# Local Print Agent (Go)

Agente local para lojas que recebem jobs da nuvem e imprimem em múltiplas impressoras USB (via fila local).

## Requisitos

- Go 1.22+
- Linux/Raspberry com CUPS (`lp`) ou Windows com fila local

## Executar em desenvolvimento

```bash
cd print-agent-go
go run .
```

O agente carrega automaticamente o arquivo `.env` da pasta atual.
Se existir `config.json` na pasta de execucao, ele tem prioridade sobre `.env`.

## Variáveis de ambiente

- `API_BASE_URL` (ex.: `http://localhost:8080`)
- `DEVICE_ID` (ex.: `agent-store-001`)
- `PRINT_KEY` (chave individual do tenant configurada no backend e entregue ao cliente)
- `POLL_INTERVAL_MS` opcional (default `1000`)

Use `.env.example` como base para criar o `.env`.

## Build release

Build multiplataforma (Linux/macOS):

```bash
VERSION=1.0.0 GIT_COMMIT=$(git rev-parse --short HEAD) ./build.sh
```

Build multiplataforma (Windows PowerShell):

```powershell
$env:VERSION="1.0.0"
$env:GIT_COMMIT=(git rev-parse --short HEAD)
.\build.ps1
```

Saída em `dist/`:

- `print-agent-windows-amd64.exe`
- `print-agent-linux-amd64`
- `print-agent-linux-arm64` (Raspberry Pi 64-bit)
- `print-agent-linux-armv7` (Raspberry Pi 32-bit)

## Pacote Windows para instalação

Gerar pacote pronto (exe + scripts):

Linux/macOS:

```bash
./release-windows.sh 1.0.0
```

Windows PowerShell:

```powershell
.\release-windows.ps1 -Version 1.0.0
```

Pacote gerado em `dist/windows-release/`:

- `print-agent-windows-amd64.exe`
- `.env.example`
- `install-windows.ps1`
- `uninstall-windows.ps1`
- `oneclick-installer/` (instalacao com duplo clique)

Dentro de `oneclick-installer/`:

- `Instalar Impressora Toluja.bat` (cliente clica neste arquivo)
- `install-windows.ps1` (instalador robusto)
- `toluja-print-agent.exe`
- `.env` (configuracao pre-preenchida para a loja)

No modo one-click, o instalador copia o `.env` para `C:\Program Files\Toluja\PrintAgent\.env` (sem edicao manual do cliente).

## Instalação no Windows (usuário final)

Abra PowerShell como **Administrador** dentro de `dist/windows-release`.

Instalar como serviço (recomendado, inicia com a máquina):

```powershell
.\install-windows.ps1 -Mode service
```

Instalar como tarefa de startup (fallback):

```powershell
.\install-windows.ps1 -Mode startup
```

A instalação copia arquivos para:

- `C:\Program Files\Toluja\PrintAgent\toluja-print-agent.exe`
- `C:\Program Files\Toluja\PrintAgent\.env`

No instalador one-click, a configuracao fica em:

- `C:\Program Files\Toluja\PrintAgent\config.json`

Depois da instalação:

1. Edite `C:\Program Files\Toluja\PrintAgent\.env` com os dados reais.
2. Reinicie o serviço/tarefa:

```powershell
sc.exe stop TolujaPrintAgent
sc.exe start TolujaPrintAgent
```

Se estiver em modo startup:

```powershell
schtasks.exe /Run /TN TolujaPrintAgent
```

## Desinstalação no Windows

```powershell
.\uninstall-windows.ps1
```

Para remover também a pasta instalada:

```powershell
.\uninstall-windows.ps1 -RemoveFiles
```

## Instalação no Raspberry (systemd)

No Raspberry (Linux com systemd), dentro da pasta `print-agent-go`:

```bash
./build.sh
sudo ./install-raspberry.sh
```

O script:

- detecta `arm64` ou `armv7` automaticamente
- instala em `/opt/toluja/print-agent`
- copia `.env` (usa `./.env` ou `./.env.example`)
- cria e inicia o serviço `toluja-print-agent.service`

Comandos úteis:

```bash
systemctl status toluja-print-agent.service --no-pager
journalctl -u toluja-print-agent.service -f
```

Se quiser sobrescrever o `.env` já instalado:

```bash
sudo ./install-raspberry.sh --force-env
```

## Canais suportados no job

- `CUPS`: `destination` = nome da fila CUPS (Linux/Raspberry)
- `WINDOWS_QUEUE`: `destination` = nome da fila do Windows

Cada job pode ter várias `deliveries`; o agente imprime todas e envia ACK por `deliveryId`.
