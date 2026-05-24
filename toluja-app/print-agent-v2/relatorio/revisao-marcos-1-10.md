# Revisao - Marcos 1 a 10

## Resultado da revisao

- Marcos 1 a 5 estao implementados no agente Java.
- Marco 6 tem empacotamento Windows definido com Inno Setup e WinSW.
- Marco 7 tem pacote por cliente via manifest/script local.
- Marco 8 tem `--status`, `--diagnostics-zip` e persistencia local de estado.
- Marco 9 tem empacotamento Linux `.tar.gz`, `install.sh` e systemd.
- Marco 10 tem Gradle Wrapper, Gson e testes automatizados.
- A pasta `manual` concentra os passo-a-passos de teste manual.

## Ajustes aplicados nesta revisao

- Pasta `manual` criada.
- Passo-a-passos `linux.md` e `windows.md` movidos para `manual/`.
- Build manual com `javac` removido da documentacao principal; o fluxo oficial agora usa `./gradlew`.
- Scripts de pacote passaram a depender do Gradle Wrapper.
- Relatorios antigos atualizados para refletir que Gson e Gradle Wrapper foram concluidos no Marco 10.
- Marco 8 implementado apos a primeira revisao.
- Marco 9 revisado apenas para incluir o uso do diagnostico no manual Linux.
- Marco 10 revisado para registrar teste automatizado do ZIP de diagnostico.

## Validacoes executadas

- `./gradlew clean test`
- `./gradlew :app:jar`
- Execucao do JAR com `--version`
- Execucao do JAR com `--config samples/config.example.json --config-check`
- Geracao do pacote Linux `.tar.gz`
- Validacao de sintaxe dos scripts Linux com `bash -n`
- Execucao do JAR dentro do pacote Linux extraido
- Execucao do JAR com `--status`
- Execucao do JAR com `--diagnostics-zip`

## Pendencias confirmadas

- Gerar instalador Windows `.exe` em ambiente Windows com Inno Setup.
- Testar instalacao/reinstalacao/remocao em Windows limpo.
- Testar impressao fisica em Windows.
- Testar CUPS real no Linux.
- Testar Raspberry se ele continuar como alvo.
- Implementar tela do backend para gerar pacote por cliente/loja.
- Criar endpoint backend de health/heartbeat para diagnostico mais preciso.

## Decisoes mantidas

- Um agente por loja.
- Jobs sempre online.
- Configuracao remota pelo backend fora do escopo atual.
- Pacote por cliente/loja nao precisa expirar.
- Assinatura digital do instalador nao e obrigatoria no inicio.
