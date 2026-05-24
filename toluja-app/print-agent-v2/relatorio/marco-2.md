# Relatorio - Marco 2

## Pontos resolvidos

- Definido o modelo inicial do `config.json`.
- Implementado `ConfigLoader`.
- Implementados modelos `AgentConfig` e `PrinterConfig`.
- Validacao dos campos obrigatorios:
  - `apiBaseUrl`
  - `tenantId`
  - `storeId`
  - `deviceId`
  - `printKey`
  - `pollIntervalMs`
- Validacao da lista de impressoras.
- Validacao dos canais `WINDOWS_QUEUE` e `CUPS`.
- Suporte a `--config` para informar caminho customizado.
- Caminho padrao Windows definido como `C:\ProgramData\Toluja\PrintAgent\config.json`.
- Caminho padrao Linux definido como `/etc/toluja/print-agent/config.json`.
- Mascara de `printKey` implementada para exibicao segura.

## Dificuldades encontradas

- Java puro nao possui parser JSON nativo simples para esse uso.
- Para manter o agente leve e sem dependencias externas neste inicio, foi criado um parser JSON pequeno no pacote `json`.
- A deteccao real de impressoras ainda nao faz parte deste marco e ficara para o Marco 5.

## Pontos de melhoria

- Substituir o parser JSON interno por Gson quando o Gradle Wrapper estiver disponivel.
- O parser interno fica apenas como solucao provisoria dos Marcos 1 e 2.
- Adicionar testes automatizados para validacao de configuracao no Marco 10.
- Configuracao atualizada pelo backend sem reinstalacao nao sera implementada no momento.

## Status posterior

- Deteccao local de impressoras foi implementada no Marco 5.
- Parser JSON interno foi substituido por Gson no Marco 10.
- Testes automatizados para validacao de configuracao foram adicionados no Marco 10.
- Configuracao remota pelo backend permanece fora do escopo atual.
