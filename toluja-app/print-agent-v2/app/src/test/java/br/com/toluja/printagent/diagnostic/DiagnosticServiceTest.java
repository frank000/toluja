package br.com.toluja.printagent.diagnostic;

import br.com.toluja.printagent.config.AgentConfig;
import br.com.toluja.printagent.config.PrinterConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDiagnosticsZipWithMaskedConfig() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            Path configPath = tempDir.resolve("config.json");
            Files.writeString(configPath, """
                    {
                      "apiBaseUrl": "http://127.0.0.1:%d",
                      "tenantId": "tenant-1",
                      "storeId": "store-1",
                      "deviceId": "device-1",
                      "printKey": "secret-print-key",
                      "pollIntervalMs": 1000,
                      "printers": [
                        {
                          "id": "default",
                          "name": "Default",
                          "channel": "CUPS",
                          "destination": "default"
                        }
                      ]
                    }
                    """.formatted(server.getAddress().getPort()), StandardCharsets.UTF_8);

            AgentConfig config = new AgentConfig(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "tenant-1",
                    "store-1",
                    "device-1",
                    "secret-print-key",
                    1000,
                    1000,
                    1,
                    0,
                    1000,
                    List.of(new PrinterConfig("default", "Default", "CUPS", "default"))
            );
            RuntimeStateStore stateStore = new RuntimeStateStore(tempDir.resolve("state.json"));
            stateStore.recordError("PRINT", "Falha de teste");

            Path zipPath = new DiagnosticService(config, configPath, stateStore)
                    .createDiagnosticsZip(tempDir.resolve("diagnostics.zip"));

            String maskedConfig = readZipEntry(zipPath, "config.masked.json");
            String status = readZipEntry(zipPath, "status.txt");
            assertFalse(maskedConfig.contains("secret-print-key"));
            assertTrue(maskedConfig.contains("secr...-key"));
            assertTrue(status.contains("Servidor respondeu HTTP 200"));
            assertTrue(status.contains("Falha de teste"));
        } finally {
            server.stop(0);
        }
    }

    private String readZipEntry(Path zipPath, String entryName) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            return new String(zipFile.getInputStream(zipFile.getEntry(entryName)).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
