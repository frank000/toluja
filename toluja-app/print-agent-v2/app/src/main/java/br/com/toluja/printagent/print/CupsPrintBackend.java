package br.com.toluja.printagent.print;

import br.com.toluja.printagent.api.dto.JobDelivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class CupsPrintBackend implements PrintBackend {
    @Override
    public String channel() {
        return "CUPS";
    }

    @Override
    public void print(JobDelivery delivery, byte[] payload) throws PrintBackendException {
        ProcessBuilder builder = new ProcessBuilder(
                "lp",
                "-d",
                delivery.destination(),
                "-n",
                Integer.toString(delivery.copies())
        );
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            try (var stdin = process.getOutputStream()) {
                stdin.write(payload);
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new PrintBackendException("Timeout ao executar lp para fila CUPS: " + delivery.destination());
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new PrintBackendException(
                        "Falha no lp para fila CUPS " + delivery.destination() + ": " + output
                );
            }
        } catch (IOException ex) {
            throw new PrintBackendException("Comando lp nao disponivel ou falhou", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PrintBackendException("Impressao CUPS interrompida", ex);
        }
    }
}
