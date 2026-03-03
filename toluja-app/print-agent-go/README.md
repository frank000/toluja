# Local Print Agent (Go)

Agente local para lojas que recebem jobs da nuvem e imprimem em múltiplas impressoras USB (via fila local).

## Requisitos

- Go 1.22+
- Linux/Raspberry com CUPS (`lp`) ou Windows com fila local

## Executar

```bash
cd print-agent-go
go run .
```

## Variáveis de ambiente

- `API_BASE_URL` (ex.: `http://localhost:8080`)
- `DEVICE_ID` (ex.: `agent-store-001`)
- `PRINT_KEY` (chave individual do tenant configurada no backend e entregue ao cliente)
- `POLL_INTERVAL_MS` opcional (default `1000`)

Exemplo:

```bash
go run .
```

O agente carrega automaticamente o `.env` da pasta.

## Gerar executáveis

Linux/macOS:

```bash
./build.sh
```

Windows (PowerShell):

```powershell
.\build.ps1
```

Saída em `dist/`:

- `print-agent-windows-amd64.exe`
- `print-agent-linux-amd64`
- `print-agent-linux-arm64` (Raspberry Pi 64-bit)
- `print-agent-linux-armv7` (Raspberry Pi 32-bit)

## Canais suportados no job

- `CUPS`: `destination` = nome da fila CUPS (Linux/Raspberry)
- `WINDOWS_QUEUE`: `destination` = nome da fila do Windows

Cada job pode ter várias `deliveries`; o agente imprime todas e envia ACK por `deliveryId`.
