package br.com.toluja.printagent.diagnostic;

import br.com.toluja.printagent.Version;
import br.com.toluja.printagent.config.AgentConfig;
import br.com.toluja.printagent.logging.AgentLogging;
import br.com.toluja.printagent.print.DetectedPrinter;
import br.com.toluja.printagent.print.PrinterDetector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DiagnosticService {
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final AgentConfig config;
    private final Path configPath;
    private final RuntimeStateStore stateStore;

    public DiagnosticService(AgentConfig config, Path configPath, RuntimeStateStore stateStore) {
        this.config = config;
        this.configPath = configPath;
        this.stateStore = stateStore;
    }

    public String buildStatusText() {
        RuntimeState state = stateStore.read();
        List<DetectedPrinter> printers = PrinterDetector.listPrinters();
        ConnectivityResult connectivity = testConnectivity();

        StringBuilder text = new StringBuilder();
        text.append("Toluja Print Agent - Status\n");
        text.append("Versao       : ").append(Version.display()).append('\n');
        text.append("Config       : ").append(configPath.toAbsolutePath()).append('\n');
        text.append("API          : ").append(config.apiBaseUrl()).append('\n');
        text.append("Tenant       : ").append(config.tenantId()).append('\n');
        text.append("Loja         : ").append(config.storeId()).append('\n');
        text.append("Device       : ").append(config.deviceId()).append('\n');
        text.append("State        : ").append(stateStore.statePath().toAbsolutePath()).append('\n');
        text.append("Logs         : ").append(AgentLogging.logsDir().toAbsolutePath()).append('\n');
        text.append("Conexao      : ").append(connectivity.message()).append('\n');
        text.append("Ultimo job   : ").append(valueOrDash(state.lastJobId()));
        if (state.lastJobAt() != null) {
            text.append(" em ").append(state.lastJobAt());
        }
        text.append('\n');
        text.append("Ultimo ACK   : ").append(valueOrDash(state.lastAckStatus()));
        if (state.lastAckAt() != null) {
            text.append(" em ").append(state.lastAckAt());
        }
        text.append('\n');
        text.append("Ultimo erro  : ").append(valueOrDash(state.lastErrorMessage()));
        if (state.lastErrorAt() != null) {
            text.append(" em ").append(state.lastErrorAt());
        }
        text.append('\n');
        text.append("Impressoras detectadas: ").append(printers.size()).append('\n');
        for (DetectedPrinter printer : printers) {
            text.append("  - ").append(printer.name()).append(" (").append(printer.source()).append(")\n");
        }
        return text.toString();
    }

    public Path createDiagnosticsZip(Path outputPath) throws IOException {
        Path target = outputPath == null ? defaultZipPath() : outputPath;
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            addText(zip, "status.txt", buildStatusText());
            addText(zip, "system.txt", systemText());
            addText(zip, "printers.txt", printersText());
            addText(zip, "config.masked.json", maskedConfig());
            addText(zip, "state.json", GSON.toJson(stateStore.read()));
            addLogs(zip);
        }
        return target.toAbsolutePath();
    }

    private ConnectivityResult testConnectivity() {
        try {
            Duration timeout = Duration.ofMillis(Math.min(config.httpTimeoutMs(), 3_000));
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.apiBaseUrl()))
                    .timeout(timeout)
                    .GET()
                    .header("X-Print-Key", config.printKey())
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return new ConnectivityResult("Servidor respondeu HTTP " + response.statusCode());
        } catch (IOException ex) {
            return new ConnectivityResult("Falha de conexao: " + exceptionMessage(ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ConnectivityResult("Teste interrompido");
        } catch (IllegalArgumentException ex) {
            return new ConnectivityResult("URL invalida: " + ex.getMessage());
        }
    }

    private String maskedConfig() {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8));
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                root.addProperty("printKey", config.maskedPrintKey());
            }
            return GSON.toJson(parsed);
        } catch (IOException | JsonSyntaxException ex) {
            return "{\n  \"error\": \"Nao foi possivel gerar config mascarada: "
                    + escapeJson(ex.getMessage()) + "\"\n}\n";
        }
    }

    private String systemText() {
        return """
                Sistema operacional: %s
                Versao do SO       : %s
                Arquitetura        : %s
                Java               : %s
                Usuario            : %s
                Diretorio atual    : %s
                Data               : %s
                """.formatted(
                System.getProperty("os.name", ""),
                System.getProperty("os.version", ""),
                System.getProperty("os.arch", ""),
                System.getProperty("java.version", ""),
                System.getProperty("user.name", ""),
                Path.of("").toAbsolutePath(),
                OffsetDateTime.now()
        );
    }

    private String printersText() {
        List<DetectedPrinter> printers = PrinterDetector.listPrinters();
        if (printers.isEmpty()) {
            return "Nenhuma impressora detectada.\n";
        }
        StringBuilder text = new StringBuilder();
        for (DetectedPrinter printer : printers) {
            text.append(printer.name()).append(" | ").append(printer.source()).append('\n');
        }
        return text.toString();
    }

    private void addLogs(ZipOutputStream zip) throws IOException {
        Path logsDir = AgentLogging.logsDir();
        if (!Files.isDirectory(logsDir)) {
            addText(zip, "logs/SEM_LOGS.txt", "Diretorio de logs nao encontrado: " + logsDir.toAbsolutePath() + "\n");
            return;
        }

        try (Stream<Path> paths = Files.list(logsDir)) {
            List<Path> logs = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().endsWith(".lck"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            if (logs.isEmpty()) {
                addText(zip, "logs/SEM_LOGS.txt", "Nenhum arquivo de log encontrado.\n");
                return;
            }
            for (Path log : logs) {
                addFile(zip, log, "logs/" + log.getFileName());
            }
        }
    }

    private void addFile(ZipOutputStream zip, Path file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private void addText(ZipOutputStream zip, String entryName, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private Path defaultZipPath() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
                .format(OffsetDateTime.now());
        return Path.of("diagnostics", "toluja-print-agent-diagnostics-" + timestamp + ".zip");
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String exceptionMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private record ConnectivityResult(String message) {
    }
}
