package com.toluja.app.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantAgentPackageService {

    private static final String WINDOWS_EMBEDDED_CONFIG_MARKER = "\n--TOLUJA-PRINT-AGENT-CONFIG-V1--\n";

    private final TenantRepository tenantRepository;
    private final PrintKeyService printKeyService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${app.print-agent.windows-template:classpath:print-agent/windows/toluja-print-agent-template.exe}")
    private String windowsTemplateLocation;

    @Value("${app.print-agent.linux-template:classpath:print-agent/linux/toluja-print-agent-linux-template.tar.gz}")
    private String linuxTemplateLocation;

    public GeneratedPackage generateWindowsPackage(String tenantId, String apiBaseUrl) {
        Tenant tenant = findTenant(tenantId);
        String printKey = rotatePrintKey(tenant);
        String normalizedApiBaseUrl = normalizeApiBaseUrl(apiBaseUrl);
        String storeSlug = defaultStoreSlug(tenant);
        String deviceId = defaultDeviceId(tenant);

        Map<String, Object> config = buildConfig(
                normalizedApiBaseUrl,
                tenant.getTenantId(),
                storeSlug,
                deviceId,
                printKey,
                List.of(printerConfig("balcao", "Balcao", "WINDOWS_QUEUE", "ALTERAR_NOME_DA_IMPRESSORA_WINDOWS"))
        );

        return new GeneratedPackage(
                "toluja-print-agent-windows-" + slug(tenant.getTenantId()) + ".exe",
                windowsExecutableWithEmbeddedConfig(json(config)),
                "application/octet-stream"
        );
    }

    public GeneratedPackage generateLinuxPackage(String tenantId, String apiBaseUrl) {
        Tenant tenant = findTenant(tenantId);
        String printKey = rotatePrintKey(tenant);
        String normalizedApiBaseUrl = normalizeApiBaseUrl(apiBaseUrl);
        String storeSlug = defaultStoreSlug(tenant);
        String deviceId = defaultDeviceId(tenant);

        Map<String, Object> config = buildConfig(
                normalizedApiBaseUrl,
                tenant.getTenantId(),
                storeSlug,
                deviceId,
                printKey,
                List.of(printerConfig("balcao", "Balcao", "CUPS", "ALTERAR_FILA_CUPS"))
        );

        String packageRoot = "toluja-print-agent-linux-" + slug(tenant.getTenantId());
        return new GeneratedPackage(
                packageRoot + ".tar.gz",
                linuxPackageWithConfig(packageRoot, json(config)),
                "application/gzip"
        );
    }

    private byte[] windowsExecutableWithEmbeddedConfig(String configJson) {
        byte[] template = loadTemplate(windowsTemplateLocation, "template Windows do print agent");
        byte[] encodedConfig = Base64.getEncoder().encode(configJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                template.length + WINDOWS_EMBEDDED_CONFIG_MARKER.length() + encodedConfig.length + 1
        );
        try {
            outputStream.write(template);
            outputStream.write(WINDOWS_EMBEDDED_CONFIG_MARKER.getBytes(StandardCharsets.UTF_8));
            outputStream.write(encodedConfig);
            outputStream.write('\n');
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao montar executavel Windows do agente", ex);
        }
        return outputStream.toByteArray();
    }

    private byte[] linuxPackageWithConfig(String packageRoot, String configJson) {
        byte[] template = loadTemplate(linuxTemplateLocation, "template Linux do print agent");
        boolean configWritten = false;

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(
                        new ByteArrayInputStream(template)
                );
                TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream);
                GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(outputStream);
                TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)
        ) {
            tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                String targetName = normalizePackageEntryName(entry.getName(), packageRoot);
                if (targetName.isBlank()) {
                    continue;
                }

                byte[] content = entry.isDirectory() ? new byte[0] : tarInputStream.readAllBytes();
                if (targetName.endsWith("/config/config.json")) {
                    content = configJson.getBytes(StandardCharsets.UTF_8);
                    configWritten = true;
                }

                TarArchiveEntry targetEntry = copyEntryMetadata(entry, targetName, content.length);
                tarOutputStream.putArchiveEntry(targetEntry);
                if (!entry.isDirectory()) {
                    tarOutputStream.write(content);
                }
                tarOutputStream.closeArchiveEntry();
            }

            if (!configWritten) {
                byte[] content = configJson.getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry targetEntry = new TarArchiveEntry(packageRoot + "/config/config.json");
                targetEntry.setMode(0640);
                targetEntry.setModTime(Date.from(Instant.now()));
                targetEntry.setSize(content.length);
                tarOutputStream.putArchiveEntry(targetEntry);
                tarOutputStream.write(content);
                tarOutputStream.closeArchiveEntry();
            }

            tarOutputStream.finish();
            gzipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao montar pacote Linux do agente", ex);
        }
    }

    private TarArchiveEntry copyEntryMetadata(TarArchiveEntry source, String targetName, int contentLength) {
        TarArchiveEntry target = new TarArchiveEntry(targetName, source.getLinkFlag());
        target.setMode(source.getMode());
        target.setModTime(source.getModTime());
        target.setUserId(source.getLongUserId());
        target.setGroupId(source.getLongGroupId());
        target.setUserName(source.getUserName());
        target.setGroupName(source.getGroupName());
        target.setLinkName(source.getLinkName());
        if (!source.isDirectory()) {
            target.setSize(contentLength);
        }
        return target;
    }

    private String normalizePackageEntryName(String originalName, String packageRoot) {
        String normalized = originalName == null ? "" : originalName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            return "";
        }

        int separatorIndex = normalized.indexOf('/');
        if (separatorIndex < 0) {
            return packageRoot;
        }
        return packageRoot + normalized.substring(separatorIndex);
    }

    private byte[] loadTemplate(String location, String description) {
        if (location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, description + " nao configurado");
        }

        Resource resource = location.contains(":")
                ? resourceLoader.getResource(location)
                : new FileSystemResource(location);
        if (!resource.exists()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    description + " nao encontrado em " + location
            );
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler " + description, ex);
        }
    }

    private Tenant findTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant nao encontrado"));
    }

    private String rotatePrintKey(Tenant tenant) {
        for (int i = 0; i < 10; i++) {
            String candidate = printKeyService.generate();
            String hash = printKeyService.hash(candidate);
            if (!tenantRepository.existsByPrintKeyHash(hash)) {
                tenant.setPrintKeyHash(hash);
                tenantRepository.save(tenant);
                return candidate;
            }
        }
        throw new IllegalStateException("Falha ao gerar print key unica");
    }

    private Map<String, Object> buildConfig(String apiBaseUrl,
                                            String tenantId,
                                            String storeId,
                                            String deviceId,
                                            String printKey,
                                            List<Map<String, Object>> printers) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("apiBaseUrl", apiBaseUrl);
        config.put("tenantId", tenantId);
        config.put("storeId", storeId);
        config.put("deviceId", deviceId);
        config.put("printKey", printKey);
        config.put("pollIntervalMs", 1000);
        config.put("httpTimeoutMs", 20000);
        config.put("apiRetryAttempts", 3);
        config.put("apiRetryBackoffMs", 500);
        config.put("printTimeoutMs", 30000);
        config.put("printers", printers);
        return config;
    }

    private Map<String, Object> printerConfig(String id, String name, String channel, String destination) {
        Map<String, Object> printer = new LinkedHashMap<>();
        printer.put("id", id);
        printer.put("name", name);
        printer.put("channel", channel);
        printer.put("destination", destination);
        return printer;
    }

    private String json(Map<String, Object> payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar pacote do agente", ex);
        }
    }

    private String normalizeApiBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apiBaseUrl invalida para o pacote");
        }
        String normalized = apiBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String defaultStoreSlug(Tenant tenant) {
        String base = slug(tenant.getTenantId());
        return base.startsWith("loja-") ? base : "loja-" + base;
    }

    private String defaultDeviceId(Tenant tenant) {
        return "agent-" + slug(tenant.getTenantId());
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "tenant" : normalized;
    }

    public record GeneratedPackage(String fileName, byte[] content, String contentType) {
    }
}
