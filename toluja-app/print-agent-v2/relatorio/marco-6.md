# Relatorio - Marco 6

## Pontos resolvidos

- Instalador escolhido para o MVP Windows: Inno Setup.
- Criada estrutura `packaging/windows`.
- Criado template WinSW `TolujaPrintAgentService.xml`.
- Criado script `build-client-package.ps1` para montar staging Windows.
- Script baixa WinSW em versao fixa (`2.12.0`) para o pacote de build.
- Layout de instalacao definido:
  - app em `C:\Program Files\Toluja\PrintAgent`
  - config/logs em `C:\ProgramData\Toluja\PrintAgent`
- Script gera runtime reduzido com `jlink`.
- Criado script Inno Setup `toluja-print-agent.iss`.
- Definida geracao de instalador `.exe` via Inno Setup.
- Instalador registra servico `TolujaPrintAgent`.
- Servico configurado com start automatico e restart em falha via WinSW.
- Desinstalacao configurada pelo Inno Setup com parada e remocao do servico.

## Dificuldades encontradas

- O ambiente atual e Linux; nao foi possivel executar Inno Setup nem validar instalacao real no Windows.
- O `.exe` final ainda precisa ser gerado em ambiente Windows com Inno Setup instalado.
- O runtime `jlink` para pacote Windows deve ser gerado em ambiente Windows com JDK 21.
- WinSW e baixado pelo script em tempo de build; se o ambiente estiver offline, o arquivo precisa estar previamente em `build\windows-package\downloads`.

## Pontos de melhoria

- Testar instalacao em Windows limpo.
- Testar reinstalacao por cima.
- Testar remocao completa.
- Assinar o instalador futuramente se antivirus/SmartScreen gerarem atrito.
- Gradle Wrapper foi gerado no Marco 10.
