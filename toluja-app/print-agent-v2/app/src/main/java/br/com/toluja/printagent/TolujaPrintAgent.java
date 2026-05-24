package br.com.toluja.printagent;

import br.com.toluja.printagent.api.dto.JobDelivery;
import br.com.toluja.printagent.config.AgentConfig;
import br.com.toluja.printagent.config.ConfigLoader;
import br.com.toluja.printagent.config.ConfigValidationException;
import br.com.toluja.printagent.config.PrinterConfig;
import br.com.toluja.printagent.diagnostic.DiagnosticService;
import br.com.toluja.printagent.diagnostic.RuntimeStateStore;
import br.com.toluja.printagent.job.JobPoller;
import br.com.toluja.printagent.logging.AgentLogging;
import br.com.toluja.printagent.print.DetectedPrinter;
import br.com.toluja.printagent.print.PrintDispatcher;
import br.com.toluja.printagent.print.PrintResult;
import br.com.toluja.printagent.print.PrinterDetector;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public final class TolujaPrintAgent {
    private TolujaPrintAgent() {
    }

    public static void main(String[] args) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            printHelp();
            System.exit(2);
            return;
        }

        if (options.help()) {
            printHelp();
            return;
        }

        if (options.version()) {
            System.out.println(Version.display());
            return;
        }

        if (options.listPrinters()) {
            printDetectedPrinters();
            return;
        }

        AgentLogging.configure();

        try {
            Path configPath = options.configPath()
                    .orElseGet(ConfigLoader::defaultConfigPath);
            AgentConfig config = ConfigLoader.load(configPath);

            if (options.status()) {
                DiagnosticService diagnostics = new DiagnosticService(
                        config,
                        configPath,
                        RuntimeStateStore.createDefault()
                );
                System.out.print(diagnostics.buildStatusText());
                return;
            }

            if (options.diagnosticsZip()) {
                DiagnosticService diagnostics = new DiagnosticService(
                        config,
                        configPath,
                        RuntimeStateStore.createDefault()
                );
                Path zipPath = diagnostics.createDiagnosticsZip(options.diagnosticsZipPath().orElse(null));
                System.out.println("ZIP de diagnostico gerado: " + zipPath);
                return;
            }

            if (options.configCheck()) {
                printConfigCheck(configPath, config);
                return;
            }

            if (options.testPrint()) {
                runTestPrint(config, options.testPrinterId());
                return;
            }

            System.out.println(Version.display());
            System.out.println("Config carregada: " + configPath.toAbsolutePath());
            JobPoller poller = JobPoller.create(config);
            if (options.once()) {
                poller.runOnce();
                return;
            }
            poller.runUntilStopped();
        } catch (ConfigValidationException ex) {
            System.err.println("Configuracao invalida: " + ex.getMessage());
            System.exit(2);
        } catch (Exception ex) {
            System.err.println("Falha ao iniciar agente: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void printConfigCheck(Path configPath, AgentConfig config) {
        System.out.println("Config OK");
        System.out.println("Arquivo      : " + configPath.toAbsolutePath());
        System.out.println("API          : " + config.apiBaseUrl());
        System.out.println("Tenant       : " + config.tenantId());
        System.out.println("Loja         : " + config.storeId());
        System.out.println("Device       : " + config.deviceId());
        System.out.println("Print key    : " + config.maskedPrintKey());
        System.out.println("Polling      : " + config.pollIntervalMs() + "ms");
        System.out.println("HTTP timeout : " + config.httpTimeoutMs() + "ms");
        System.out.println("API retries  : " + config.apiRetryAttempts());
        System.out.println("Print timeout: " + config.printTimeoutMs() + "ms");
        System.out.println("Impressoras  : " + config.printers().size());
        config.printers().forEach(printer -> System.out.printf(
                "  - %s | %s | %s | %s%n",
                printer.id(),
                printer.name(),
                printer.channel(),
                printer.destination()
        ));
    }

    private static void printDetectedPrinters() {
        List<DetectedPrinter> printers = PrinterDetector.listPrinters();
        if (printers.isEmpty()) {
            System.out.println("Nenhuma impressora detectada.");
            System.out.println("Fallback: configure manualmente o campo destination no config.json.");
            return;
        }

        System.out.println("Impressoras detectadas:");
        for (DetectedPrinter printer : printers) {
            System.out.println("  - " + printer.name() + " (" + printer.source() + ")");
        }
    }

    private static void runTestPrint(AgentConfig config, Optional<String> printerId) {
        PrinterConfig printer = selectPrinter(config, printerId);
        String text = """
                === TESTE DE IMPRESSAO TOLUJA ===
                Device: %s
                Loja: %s
                Impressora: %s
                Data: %s
                ================================

                """.formatted(
                config.deviceId(),
                config.storeId(),
                printer.name(),
                OffsetDateTime.now()
        );

        JobDelivery delivery = new JobDelivery(
                "test-print",
                printer.id(),
                printer.name(),
                printer.channel(),
                printer.destination(),
                1
        );
        PrintResult result = PrintDispatcher.create(config)
                .print(delivery, text.getBytes(StandardCharsets.UTF_8));

        if (result.success()) {
            System.out.println("Teste de impressao enviado com sucesso para: " + printer.name());
            return;
        }

        System.err.println("Falha no teste de impressao: " + result.errorMessage());
        System.exit(1);
    }

    private static PrinterConfig selectPrinter(AgentConfig config, Optional<String> printerId) {
        if (printerId.isEmpty()) {
            return config.printers().getFirst();
        }

        return config.printers().stream()
                .filter(printer -> printer.id().equalsIgnoreCase(printerId.get()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Impressora configurada nao encontrada: " + printerId.get()
                ));
    }

    private static void printHelp() {
        System.out.println("""
                Toluja Print Agent v2

                Uso:
                  java -jar toluja-print-agent.jar [opcoes]

                Opcoes:
                  --help             Mostra esta ajuda.
                  --version          Mostra a versao do agente.
                  --config PATH      Define o caminho do config.json.
                  --config-check     Valida e resume a configuracao.
                  --status           Mostra diagnostico rapido do agente.
                  --diagnostics-zip [PATH]
                                     Gera ZIP de diagnostico para suporte.
                  --once             Executa uma consulta/processamento e encerra.
                  --list-printers    Lista impressoras locais detectadas.
                  --test-print [ID]  Envia pagina de teste para impressora configurada.
                """);
    }

    private record CliOptions(
            boolean help,
            boolean version,
            boolean configCheck,
            boolean status,
            boolean diagnosticsZip,
            boolean once,
            boolean listPrinters,
            boolean testPrint,
            Optional<String> testPrinterId,
            Optional<Path> diagnosticsZipPath,
            Optional<Path> configPath
    ) {
        static CliOptions parse(String[] args) {
            boolean help = false;
            boolean version = false;
            boolean configCheck = false;
            boolean status = false;
            boolean diagnosticsZip = false;
            boolean once = false;
            boolean listPrinters = false;
            boolean testPrint = false;
            String testPrinterId = null;
            Path diagnosticsZipPath = null;
            Path configPath = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--help", "-h" -> help = true;
                    case "--version" -> version = true;
                    case "--config-check" -> configCheck = true;
                    case "--status" -> status = true;
                    case "--diagnostics-zip" -> {
                        diagnosticsZip = true;
                        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                            diagnosticsZipPath = Path.of(args[++i]);
                        }
                    }
                    case "--once" -> once = true;
                    case "--list-printers" -> listPrinters = true;
                    case "--test-print" -> {
                        testPrint = true;
                        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                            testPrinterId = args[++i];
                        }
                    }
                    case "--config" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--config exige um caminho");
                        }
                        configPath = Path.of(args[++i]);
                    }
                    default -> throw new IllegalArgumentException("Opcao desconhecida: " + arg);
                }
            }

            return new CliOptions(
                    help,
                    version,
                    configCheck,
                    status,
                    diagnosticsZip,
                    once,
                    listPrinters,
                    testPrint,
                    Optional.ofNullable(testPrinterId),
                    Optional.ofNullable(diagnosticsZipPath),
                    Optional.ofNullable(configPath)
            );
        }
    }
}
