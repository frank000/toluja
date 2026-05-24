# Relatorio - Marco 1

## Pontos resolvidos

- Criada a estrutura `app/` para o agente Java.
- Configurado projeto Gradle com Java 21.
- Configurado JAR executavel com `Main-Class`.
- Criada a classe principal `TolujaPrintAgent`.
- Adicionados comandos `--help`, `--version` e `--config-check`.
- Adicionado logging simples em console e arquivo rotativo.
- Definida convencao inicial de versao via `agentVersion`/manifest do JAR.
- Criado `samples/config.example.json`.

## Dificuldades encontradas

- O ambiente local nao possui `gradle` instalado.
- A tentativa de baixar uma distribuicao Gradle para gerar wrapper nao concluiu em tempo util.
- A validacao foi feita com `javac` e `jar` diretamente, usando Java 21 disponivel no ambiente.

## Pontos de melhoria

- Gerar e versionar o Gradle Wrapper quando a rede estiver estavel.
- Adicionar tarefa de build para runtime reduzido com `jlink`.
- Evoluir o logging para incluir contexto de job quando o Marco 4 for implementado.

## Status posterior

- Logging com contexto basico de job foi adicionado no Marco 4.
- `jlink` foi implementado no fluxo de empacotamento do Marco 6.
- Gradle Wrapper foi gerado no Marco 10.
