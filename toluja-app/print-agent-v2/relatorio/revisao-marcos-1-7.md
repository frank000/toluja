# Revisao - Marcos 1 a 7

## Resultado da revisao

- Marcos 1 a 5 estao implementados no codigo do agente.
- Marco 6 tem estrutura de empacotamento Windows, WinSW, Inno Setup e script de pacote.
- Marco 7 tem manifest por cliente, exemplo e documentacao de fluxo para suporte.
- Nao foi identificado ajuste funcional obrigatorio no agente Java durante esta revisao.

## Ajustes aplicados

- Atualizado o Marco 1 para registrar que `jlink` foi implementado no fluxo do Marco 6.
- Ajustado o backlog do Marco 6 para diferenciar:
  - definicao/script do instalador `.exe`, ja implementado;
  - geracao real do `.exe` em ambiente Windows com Inno Setup, ainda pendente.
- Atualizado o relatorio do Marco 6 com a mesma separacao.

## Pendencias confirmadas

- Gerar o instalador `.exe` em Windows com Inno Setup.
- Testar instalacao em Windows limpo.
- Testar reinstalacao por cima.
- Testar remocao completa.
- Implementar tela do backend para gerar pacote por cliente/loja.
- Validar impressao fisica em Windows e Linux/CUPS.

## Status posterior

- Gradle Wrapper foi gerado no Marco 10.
- Parser JSON interno foi substituido por Gson no Marco 10.

## Decisoes mantidas

- Um agente por loja.
- Jobs sempre online.
- Configuracao remota pelo backend fora do escopo atual.
- Pacote por cliente/loja nao precisa expirar.
- Assinatura digital do instalador nao e obrigatoria no inicio.
