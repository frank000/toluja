# Relatorio - Marco 10

## Pontos resolvidos

- Gradle Wrapper gerado e versionado.
- Parser JSON interno removido do caminho principal.
- Gson adicionado como dependencia.
- `ConfigLoader` migrado para Gson.
- Parsing/escrita JSON da API migrados para Gson.
- JAR ajustado para incluir dependencias de runtime.
- Criados testes automatizados para `ConfigLoader`.
- Criados testes automatizados para parsing dos DTOs.
- Criado teste do cliente HTTP com servidor fake.
- Testado ACK de sucesso.
- Testado ACK parcial/com erro.
- Testado canal desconhecido.
- Testado `copies` invalido.
- Testada mascara de segredo.
- Testada geracao de ZIP de diagnostico apos implementacao do Marco 8.
- Criada pasta `manual`.
- Movidos os passos manuais de Windows e Linux para `manual/`.

## Dificuldades encontradas

- O primeiro teste de serializacao de ACK mostrou que Gson omite `null` por padrao.
- O `Gson` usado para API foi configurado com `serializeNulls()` para manter `errorMessage: null` no contrato.
- Apos adicionar Gson, o build manual com `javac` deixou de ser adequado; os scripts agora dependem do Gradle Wrapper.

## Validacao executada

- `./gradlew clean test`
- `./gradlew :app:jar`
- `java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --version`
- `java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --config-check`

## Pontos de melhoria

- Expandir testes de erro HTTP/retry.
- Avaliar cobertura de comandos CLI com teste de processo real.
