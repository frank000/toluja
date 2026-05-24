# Manual de Testes

Passo-a-passos simples para validar o Toluja Print Agent v2.

## Arquivos

- `linux.md`: build, instalacao e teste manual em Linux/systemd/CUPS.
- `windows.md`: geracao do instalador, instalacao, servico e teste manual em Windows.

## Ordem recomendada

1. Rodar testes automatizados:

   ```bash
   ./gradlew test
   ```

2. Validar config local:

   ```bash
   ./gradlew :app:jar
   java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --config-check
   ```

3. Validar diagnostico local:

   ```bash
   java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --status
   java -jar app/build/libs/toluja-print-agent-0.1.0-dev.jar --config samples/config.example.json --diagnostics-zip
   ```

4. Seguir o manual da plataforma alvo.
