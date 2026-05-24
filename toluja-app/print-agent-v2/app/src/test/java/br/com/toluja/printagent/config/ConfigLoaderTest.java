package br.com.toluja.printagent.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsValidConfig() throws Exception {
        Path config = writeConfig("""
                {
                  "apiBaseUrl": "https://app.toluja.com.br",
                  "tenantId": "cliente-x",
                  "storeId": "loja-001",
                  "deviceId": "agent-loja-001",
                  "printKey": "SECRET-123456",
                  "pollIntervalMs": 1000,
                  "printers": [
                    {
                      "id": "balcao",
                      "name": "Balcao",
                      "channel": "windows_queue",
                      "destination": "IMPRESSORA1"
                    }
                  ]
                }
                """);

        AgentConfig loaded = ConfigLoader.load(config);

        assertEquals("https://app.toluja.com.br", loaded.apiBaseUrl());
        assertEquals("cliente-x", loaded.tenantId());
        assertEquals("loja-001", loaded.storeId());
        assertEquals("agent-loja-001", loaded.deviceId());
        assertEquals("SECR...3456", loaded.maskedPrintKey());
        assertEquals(1000, loaded.pollIntervalMs());
        assertEquals(20_000, loaded.httpTimeoutMs());
        assertEquals("WINDOWS_QUEUE", loaded.printers().getFirst().channel());
    }

    @Test
    void rejectsMissingRequiredField() throws Exception {
        Path config = writeConfig("""
                {
                  "apiBaseUrl": "https://app.toluja.com.br",
                  "tenantId": "cliente-x",
                  "storeId": "loja-001",
                  "printKey": "SECRET",
                  "pollIntervalMs": 1000,
                  "printers": []
                }
                """);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(config));
        assertEquals("Campo obrigatorio ausente ou vazio: deviceId", ex.getMessage());
    }

    @Test
    void rejectsInvalidChannel() throws Exception {
        Path config = writeConfig("""
                {
                  "apiBaseUrl": "https://app.toluja.com.br",
                  "tenantId": "cliente-x",
                  "storeId": "loja-001",
                  "deviceId": "agent-loja-001",
                  "printKey": "SECRET",
                  "pollIntervalMs": 1000,
                  "printers": [
                    {"id": "x", "name": "X", "channel": "RAW", "destination": "X"}
                  ]
                }
                """);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(config));
        assertEquals("Canal de impressora nao suportado em 'x': RAW", ex.getMessage());
    }

    @Test
    void masksShortSecret() {
        AgentConfig config = new AgentConfig("https://app.toluja.com.br", "t", "s", "d", "SHORT", 1000, 20000, 3, 500, 30000, java.util.List.of(
                new PrinterConfig("p", "P", "CUPS", "P")
        ));

        assertEquals("****", config.maskedPrintKey());
    }

    private Path writeConfig(String content) throws Exception {
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, content);
        return config;
    }
}
