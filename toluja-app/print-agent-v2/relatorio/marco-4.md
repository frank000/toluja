# Relatorio - Marco 4

## Pontos resolvidos

- Implementado `JobPoller`.
- Implementado loop continuo usando `pollIntervalMs`.
- Adicionado comando `--once` para executar um unico ciclo e encerrar.
- Evitado processamento concorrente com controle atomico.
- Implementada decodificacao de `payloadBase64`.
- Validado `payloadType`, aceitando `TEXT` no MVP.
- Implementado processamento de todas as `deliveries`.
- Gerado ACK por delivery.
- Garantido ACK de erro quando payload ou delivery falham.
- Registrado job processado e ACK enviado no log.
- Adicionado shutdown hook para parada limpa.

## Dificuldades encontradas

- A impressao real ainda pertence ao Marco 5. Para nao gerar falso positivo, o executor atual retorna erro explicito por delivery.
- Sem backend real rodando, nao foi feito teste de integracao com containers.
- Foi feito teste local com servidor fake para validar um ciclo completo com `--once`.

## Pontos de melhoria

- Implementar `PrintDispatcher` e backends reais de impressao no Marco 5.
- Adicionar persistencia simples de ultimo job/ultimo erro para diagnostico no Marco 8.
- Avaliar politica de reprocessamento quando o ACK falha apos o job ter sido processado.

## Status posterior

- `PrintDispatcher` e backends reais de impressao foram implementados no Marco 5.
- Persistencia de ultimo job/ultimo erro segue pendente para diagnostico no Marco 8.
- Politica de reprocessamento apos falha de ACK segue pendente.
