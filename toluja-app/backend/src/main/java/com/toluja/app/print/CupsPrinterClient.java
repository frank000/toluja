package com.toluja.app.print;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class CupsPrinterClient implements PrinterClient {

    private final int timeoutMs;

    public CupsPrinterClient(@Value("${printers.cups-timeout-ms:5000}") int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void print(String queueName, String content) throws Exception {
        Process process = new ProcessBuilder("lp", "-d", queueName).start();
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(content);
        }

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Timeout ao imprimir via CUPS (" + timeoutMs + " ms)");
        }

        int exit = process.exitValue();
        if (exit != 0) {
            throw new IllegalStateException("Falha ao imprimir via CUPS. Código de saída: " + exit);
        }
    }

    @Override
    public void print(String content) {
        throw new UnsupportedOperationException("Use print(queueName, content)");
    }
}
