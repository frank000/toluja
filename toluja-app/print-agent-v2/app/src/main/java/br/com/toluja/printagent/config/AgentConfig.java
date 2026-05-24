package br.com.toluja.printagent.config;

import java.util.List;

public record AgentConfig(
        String apiBaseUrl,
        String tenantId,
        String storeId,
        String deviceId,
        String printKey,
        int pollIntervalMs,
        int httpTimeoutMs,
        int apiRetryAttempts,
        int apiRetryBackoffMs,
        int printTimeoutMs,
        List<PrinterConfig> printers
) {
    public AgentConfig {
        printers = List.copyOf(printers);
    }

    public String maskedPrintKey() {
        if (printKey == null || printKey.isBlank()) {
            return "";
        }
        String normalized = printKey.trim();
        if (normalized.length() <= 8) {
            return "****";
        }
        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 4);
    }
}
