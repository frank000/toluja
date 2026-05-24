package com.toluja.app.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAgentPackageServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PrintKeyService printKeyService;

    private TenantAgentPackageService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        service = new TenantAgentPackageService(
                tenantRepository,
                printKeyService,
                objectMapper,
                new DefaultResourceLoader()
        );
        ReflectionTestUtils.setField(service, "windowsTemplateLocation", createWindowsTemplate().toString());
        ReflectionTestUtils.setField(service, "linuxTemplateLocation", createLinuxTemplate().toString());
    }

    @Test
    void shouldGenerateWindowsExecutableWithEmbeddedConfigAndRotatedPrintKey() throws Exception {
        Tenant tenant = tenant("cliente-x");
        when(tenantRepository.findByTenantId("cliente-x")).thenReturn(Optional.of(tenant));
        when(printKeyService.generate()).thenReturn("new-print-key");
        when(printKeyService.hash("new-print-key")).thenReturn("new-print-key-hash");
        when(tenantRepository.existsByPrintKeyHash(anyString())).thenReturn(false);

        TenantAgentPackageService.GeneratedPackage generated = service.generateWindowsPackage(
                "cliente-x",
                "http://localhost:8080/"
        );

        assertThat(generated.fileName()).isEqualTo("toluja-print-agent-windows-cliente-x.exe");
        assertThat(generated.contentType()).isEqualTo("application/octet-stream");
        assertThat(new String(generated.content(), 0, 12, StandardCharsets.UTF_8)).isEqualTo("MZ-TEMPLATE\n");

        Map<String, Object> config = readJson(embeddedWindowsConfig(generated.content()));
        assertThat(config.get("tenantId")).isEqualTo("cliente-x");
        assertThat(config.get("storeId")).isEqualTo("loja-cliente-x");
        assertThat(config.get("deviceId")).isEqualTo("agent-cliente-x");
        assertThat(config.get("apiBaseUrl")).isEqualTo("http://localhost:8080");
        assertThat(config.get("printKey")).isEqualTo("new-print-key");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getPrintKeyHash()).isEqualTo("new-print-key-hash");
    }

    @Test
    void shouldGenerateLinuxPackageWithCupsPlaceholder() throws Exception {
        Tenant tenant = tenant("cliente-y");
        when(tenantRepository.findByTenantId("cliente-y")).thenReturn(Optional.of(tenant));
        when(printKeyService.generate()).thenReturn("linux-print-key");
        when(printKeyService.hash("linux-print-key")).thenReturn("linux-print-key-hash");
        when(tenantRepository.existsByPrintKeyHash(anyString())).thenReturn(false);

        TenantAgentPackageService.GeneratedPackage generated = service.generateLinuxPackage(
                "cliente-y",
                "https://app.toluja.com.br"
        );

        assertThat(generated.fileName()).isEqualTo("toluja-print-agent-linux-cliente-y.tar.gz");
        assertThat(generated.contentType()).isEqualTo("application/gzip");
        Map<String, String> files = untarGz(generated.content());
        assertThat(files).containsKeys(
                "toluja-print-agent-linux-cliente-y/app/toluja-print-agent.jar",
                "toluja-print-agent-linux-cliente-y/config/config.json",
                "toluja-print-agent-linux-cliente-y/install.sh"
        );

        Map<String, Object> config = readJson(files.get("toluja-print-agent-linux-cliente-y/config/config.json"));
        assertThat(config.get("apiBaseUrl")).isEqualTo("https://app.toluja.com.br");
        assertThat(config.get("storeId")).isEqualTo("loja-cliente-y");

        @SuppressWarnings("unchecked")
        var printers = (java.util.List<Map<String, Object>>) config.get("printers");
        assertThat(printers).hasSize(1);
        assertThat(printers.get(0).get("channel")).isEqualTo("CUPS");
        assertThat(printers.get(0).get("destination")).isEqualTo("ALTERAR_FILA_CUPS");
    }

    private Tenant tenant(String tenantId) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setNome("Cliente");
        tenant.setAtivo(true);
        tenant.setEntregaAtiva(false);
        tenant.setPrintKeyHash("old-hash");
        return tenant;
    }

    private Path createWindowsTemplate() throws IOException {
        Path path = tempDir.resolve("toluja-print-agent-template.exe");
        Files.writeString(path, "MZ-TEMPLATE\n", StandardCharsets.UTF_8);
        return path;
    }

    private Path createLinuxTemplate() throws IOException {
        Path path = tempDir.resolve("toluja-print-agent-linux-template.tar.gz");
        try (
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(bytes);
                TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)
        ) {
            addTarDirectory(tarOutputStream, "toluja-print-agent-linux-template/");
            addTarDirectory(tarOutputStream, "toluja-print-agent-linux-template/app/");
            addTarFile(tarOutputStream, "toluja-print-agent-linux-template/app/toluja-print-agent.jar", "jar");
            addTarDirectory(tarOutputStream, "toluja-print-agent-linux-template/config/");
            addTarFile(tarOutputStream, "toluja-print-agent-linux-template/config/config.json", "{}");
            addTarFile(tarOutputStream, "toluja-print-agent-linux-template/install.sh", "#!/usr/bin/env bash\n");
            tarOutputStream.finish();
            gzipOutputStream.finish();
            Files.write(path, bytes.toByteArray());
        }
        return path;
    }

    private void addTarDirectory(TarArchiveOutputStream tarOutputStream, String name) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(0755);
        tarOutputStream.putArchiveEntry(entry);
        tarOutputStream.closeArchiveEntry();
    }

    private void addTarFile(TarArchiveOutputStream tarOutputStream, String name, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(0644);
        entry.setSize(bytes.length);
        tarOutputStream.putArchiveEntry(entry);
        tarOutputStream.write(bytes);
        tarOutputStream.closeArchiveEntry();
    }

    private String embeddedWindowsConfig(byte[] bytes) {
        String marker = "\n--TOLUJA-PRINT-AGENT-CONFIG-V1--\n";
        String content = new String(bytes, StandardCharsets.UTF_8);
        int markerIndex = content.lastIndexOf(marker);
        assertThat(markerIndex).isGreaterThanOrEqualTo(0);
        String encoded = content.substring(markerIndex + marker.length()).trim();
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private Map<String, String> untarGz(byte[] bytes) throws IOException {
        Map<String, String> files = new HashMap<>();
        try (
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(new ByteArrayInputStream(bytes));
                TarArchiveInputStream inputStream = new TarArchiveInputStream(gzipInputStream)
        ) {
            TarArchiveEntry entry;
            while ((entry = inputStream.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    files.put(entry.getName(), new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return files;
    }

    private Map<String, Object> readJson(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }
}
