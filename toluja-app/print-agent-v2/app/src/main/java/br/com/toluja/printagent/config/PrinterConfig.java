package br.com.toluja.printagent.config;

public record PrinterConfig(
        String id,
        String name,
        String channel,
        String destination
) {
}
