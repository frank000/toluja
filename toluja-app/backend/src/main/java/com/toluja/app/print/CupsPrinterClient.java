package com.toluja.app.print;

import org.springframework.stereotype.Component;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Component
public class CupsPrinterClient implements PrinterClient {

    public void print(String queueName, String content) throws Exception {
        Process process = new ProcessBuilder("lp", "-d", queueName).start();
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(content);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Falha ao imprimir via CUPS. Código de saída: " + exit);
        }
    }

    @Override
    public void print(String content) {
        throw new UnsupportedOperationException("Use print(queueName, content)");
    }
}
