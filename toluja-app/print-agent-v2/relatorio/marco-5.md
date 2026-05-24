# Relatorio - Marco 5

## Pontos resolvidos

- Criada interface `PrintBackend`.
- Criado `PrintDispatcher` como implementacao real de `PrintExecutor`.
- Implementado canal `WINDOWS_QUEUE` usando `javax.print` e fila local pelo nome.
- Implementado canal `CUPS` usando comando `lp`.
- Canal desconhecido retorna erro claro por delivery.
- `copies < 1` retorna erro claro por delivery.
- Timeout de impressao configuravel por `printTimeoutMs`.
- Impressao de texto simples passa pelo payload recebido em bytes.
- Criado comando local `--list-printers`.
- Criado comando local `--test-print [ID]`.
- Deteccao local de impressoras implementada com Java PrintService.
- Em Linux, deteccao tambem tenta `lpstat -a` quando disponivel.
- Fallback manual definido: usar `destination` no `config.json` quando a deteccao nao encontrar a impressora correta.

## Dificuldades encontradas

- A validacao real de `WINDOWS_QUEUE` precisa acontecer em maquina Windows com impressora/fila instalada.
- A validacao real de `CUPS` precisa acontecer em Linux com CUPS e fila configurada.
- `javax.print` depende do suporte local do sistema operacional e pode listar impressoras de forma diferente em cada ambiente.
- O timeout no Java cancela a espera do agente, mas o sistema operacional ainda pode manter um job de impressao ja entregue para a fila.
- O teste de integracao local validou o fluxo de ACK com erro esperado, mas nao valida impressao fisica.

## Pontos de melhoria

- Testar em Windows 10/11 com impressoras termicas reais.
- Testar em Linux com CUPS real.
- Melhorar mensagens de diagnostico para diferenciar "fila nao existe", "driver indisponivel" e "spooler parado".
- Avaliar suporte RAW/ESC-POS no futuro, conforme planejado.

## Status posterior

- Mensagem de fila Windows nao encontrada foi melhorada para mostrar filas detectadas quando houver.
- Validacao fisica em Windows e Linux segue pendente.
- Diagnostico de driver/spooler parado segue pendente para evolucao do Marco 8.
