# Relatorio - Marco 7

## Pontos resolvidos

- Definido manifest JSON de pacote por cliente.
- Criado exemplo em `samples/customer-package/customer-package.example.json`.
- Criado script local `build-client-package.ps1` para gerar pacote Windows por cliente.
- O manifest inclui:
  - `apiBaseUrl`
  - `tenantId`
  - `storeId`
  - `deviceId`
  - `printKey`
  - impressoras
- `config.json` pre-preenchido e incluido no instalador.
- Nome do instalador definido como `TolujaPrintAgent-Cliente-Loja-Setup.exe`.
- Usuario final nao precisa editar arquivo de configuracao.
- Documentado processo interno em `packaging/windows/README.md`.
- Regra de um agente por loja documentada no schema do manifest.
- Impressora detectada/manual e vinculada via `destination` no manifest/config.

## Dificuldades encontradas

- A tela do backend para gerar pacote por cliente/loja nao foi implementada neste passo.
- O fluxo atual gera pacote por script local, servindo como base para futura integracao com o backend.
- A vinculacao automatica de impressora ainda depende de o suporte confirmar o nome detectado antes de gerar o manifest.

## Pontos de melhoria

- Implementar tela administrativa no backend para gerar pacote por cliente/loja.
- Permitir que o backend gere o manifest usado pelo script de empacotamento.
- Definir processo de suporte para coletar/listar impressoras antes de fechar o pacote.
- O pacote nao precisa expirar.
- Rotacao da print key pode ficar como acao manual/futura pelo backend, se necessario.
