# Manifest de Pacote por Cliente

O script `build-client-package.ps1` recebe um JSON com os dados do pacote.

## Campos

- `clientSlug`: identificador curto do cliente para nome do instalador.
- `storeSlug`: identificador curto da loja para nome do instalador.
- `agentVersion`: versao do agente a empacotar.
- `config`: objeto que sera instalado como `config.json`.

## Config

Campos obrigatorios do `config`:

- `apiBaseUrl`
- `tenantId`
- `storeId`
- `deviceId`
- `printKey`
- `pollIntervalMs`
- `printers`

Campos opcionais com default no agente:

- `httpTimeoutMs`
- `apiRetryAttempts`
- `apiRetryBackoffMs`
- `printTimeoutMs`

## Regra de negocio

- MVP assume um agente por loja.
- O `deviceId` deve ser unico para a loja.
- O pacote ja deve sair com `printKey` e impressoras configuradas.
- Configuracao remota pelo backend nao entra no momento.
- O pacote nao precisa expirar.

## Impressoras

Cada impressora:

```json
{
  "id": "balcao",
  "name": "Balcao",
  "channel": "WINDOWS_QUEUE",
  "destination": "EPSON TM-T20X Receipt"
}
```

No Windows, `destination` deve ser o nome da fila local.
Quando a deteccao automatica nao encontrar a fila correta, o suporte deve informar o
nome manualmente no manifest.
