package br.com.toluja.printagent.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConfigLoader {
    private static final int MIN_POLL_INTERVAL_MS = 250;
    private static final int DEFAULT_HTTP_TIMEOUT_MS = 20_000;
    private static final int DEFAULT_API_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_API_RETRY_BACKOFF_MS = 500;
    private static final int DEFAULT_PRINT_TIMEOUT_MS = 30_000;
    private static final Gson GSON = new Gson();

    private ConfigLoader() {
    }

    public static Path defaultConfigPath() {
        if (isWindows()) {
            String programData = System.getenv("ProgramData");
            if (programData != null && !programData.isBlank()) {
                return Path.of(programData, "Toluja", "PrintAgent", "config.json");
            }
            return Path.of("C:\\ProgramData\\Toluja\\PrintAgent\\config.json");
        }
        return Path.of("/etc/toluja/print-agent/config.json");
    }

    public static AgentConfig load(Path path) throws ConfigValidationException {
        if (!Files.isRegularFile(path)) {
            throw new ConfigValidationException("Arquivo de configuracao nao encontrado: " + path);
        }

        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ConfigValidationException("Nao foi possivel ler config: " + path, ex);
        }

        JsonObject root;
        try {
            JsonElement parsed = GSON.fromJson(content, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new ConfigValidationException("config.json deve conter um objeto JSON");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new ConfigValidationException("JSON invalido em " + path + ": " + ex.getMessage(), ex);
        }

        AgentConfig config = new AgentConfig(
                requiredString(root, "apiBaseUrl"),
                requiredString(root, "tenantId"),
                requiredString(root, "storeId"),
                requiredString(root, "deviceId"),
                requiredString(root, "printKey"),
                requiredInt(root, "pollIntervalMs"),
                optionalInt(root, "httpTimeoutMs", DEFAULT_HTTP_TIMEOUT_MS),
                optionalInt(root, "apiRetryAttempts", DEFAULT_API_RETRY_ATTEMPTS),
                optionalInt(root, "apiRetryBackoffMs", DEFAULT_API_RETRY_BACKOFF_MS),
                optionalInt(root, "printTimeoutMs", DEFAULT_PRINT_TIMEOUT_MS),
                readPrinters(root)
        );
        validate(config);
        return config;
    }

    private static List<PrinterConfig> readPrinters(JsonObject root)
            throws ConfigValidationException {
        JsonElement rawPrinters = root.get("printers");
        if (rawPrinters == null || !rawPrinters.isJsonArray()) {
            throw new ConfigValidationException("Campo obrigatorio 'printers' deve ser uma lista");
        }

        JsonArray printers = rawPrinters.getAsJsonArray();
        List<PrinterConfig> result = new ArrayList<>();
        for (int i = 0; i < printers.size(); i++) {
            JsonElement item = printers.get(i);
            if (!item.isJsonObject()) {
                throw new ConfigValidationException("Impressora #" + (i + 1) + " deve ser um objeto");
            }
            JsonObject printer = item.getAsJsonObject();
            result.add(new PrinterConfig(
                    requiredString(printer, "id"),
                    requiredString(printer, "name"),
                    normalizeChannel(requiredString(printer, "channel")),
                    requiredString(printer, "destination")
            ));
        }
        return result;
    }

    private static void validate(AgentConfig config) throws ConfigValidationException {
        validateApiBaseUrl(config.apiBaseUrl());

        if (config.pollIntervalMs() < MIN_POLL_INTERVAL_MS) {
            throw new ConfigValidationException(
                    "pollIntervalMs deve ser maior ou igual a " + MIN_POLL_INTERVAL_MS
            );
        }
        if (config.httpTimeoutMs() < 1000) {
            throw new ConfigValidationException("httpTimeoutMs deve ser maior ou igual a 1000");
        }
        if (config.apiRetryAttempts() < 1) {
            throw new ConfigValidationException("apiRetryAttempts deve ser maior ou igual a 1");
        }
        if (config.apiRetryBackoffMs() < 0) {
            throw new ConfigValidationException("apiRetryBackoffMs deve ser maior ou igual a 0");
        }
        if (config.printTimeoutMs() < 1000) {
            throw new ConfigValidationException("printTimeoutMs deve ser maior ou igual a 1000");
        }

        if (config.printers().isEmpty()) {
            throw new ConfigValidationException("Ao menos uma impressora deve ser configurada");
        }

        for (PrinterConfig printer : config.printers()) {
            if (!printer.channel().equals("WINDOWS_QUEUE") && !printer.channel().equals("CUPS")) {
                throw new ConfigValidationException(
                        "Canal de impressora nao suportado em '" + printer.id() + "': " + printer.channel()
                );
            }
        }
    }

    private static void validateApiBaseUrl(String apiBaseUrl) throws ConfigValidationException {
        try {
            URI uri = new URI(apiBaseUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new ConfigValidationException("apiBaseUrl deve iniciar com http:// ou https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ConfigValidationException("apiBaseUrl deve conter host valido");
            }
        } catch (URISyntaxException ex) {
            throw new ConfigValidationException("apiBaseUrl invalida: " + apiBaseUrl, ex);
        }
    }

    private static String requiredString(JsonObject object, String key)
            throws ConfigValidationException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new ConfigValidationException("Campo obrigatorio ausente ou vazio: " + key);
        }
        String text = value.getAsString();
        if (text.trim().isEmpty()) {
            throw new ConfigValidationException("Campo obrigatorio ausente ou vazio: " + key);
        }
        return text.trim();
    }

    private static int requiredInt(JsonObject object, String key)
            throws ConfigValidationException {
        JsonElement value = object.get(key);
        if (value == null) {
            throw new ConfigValidationException("Campo obrigatorio ausente: " + key);
        }
        return jsonInt(value, key);
    }

    private static int optionalInt(JsonObject object, String key, int defaultValue)
            throws ConfigValidationException {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        return jsonInt(value, key);
    }

    private static int jsonInt(JsonElement value, String key) throws ConfigValidationException {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            try {
                return value.getAsInt();
            } catch (NumberFormatException ex) {
                throw new ConfigValidationException("Campo '" + key + "' deve ser numerico", ex);
            }
        }
        throw new ConfigValidationException("Campo '" + key + "' deve ser numerico");
    }

    private static String normalizeChannel(String channel) {
        return channel.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
