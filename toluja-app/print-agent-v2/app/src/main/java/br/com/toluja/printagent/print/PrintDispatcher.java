package br.com.toluja.printagent.print;

import br.com.toluja.printagent.api.dto.JobDelivery;
import br.com.toluja.printagent.config.AgentConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PrintDispatcher implements PrintExecutor {
    private final Map<String, PrintBackend> backends;
    private final int timeoutMs;

    public PrintDispatcher(List<PrintBackend> backends, int timeoutMs) {
        this.backends = new HashMap<>();
        for (PrintBackend backend : backends) {
            this.backends.put(normalize(backend.channel()), backend);
        }
        this.timeoutMs = timeoutMs;
    }

    public static PrintDispatcher create(AgentConfig config) {
        return new PrintDispatcher(
                List.of(new WindowsQueuePrintBackend(), new CupsPrintBackend()),
                config.printTimeoutMs()
        );
    }

    @Override
    public PrintResult print(JobDelivery delivery, byte[] payload) {
        if (delivery.copies() < 1) {
            return PrintResult.failed("copies deve ser maior ou igual a 1");
        }

        PrintBackend backend = backends.get(normalize(delivery.channel()));
        if (backend == null) {
            return PrintResult.failed("Canal de impressao nao suportado: " + delivery.channel());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                backend.print(delivery, payload);
                return null;
            }).get(timeoutMs, TimeUnit.MILLISECONDS);
            return PrintResult.ok();
        } catch (TimeoutException ex) {
            return PrintResult.failed("Timeout de impressao apos " + timeoutMs + "ms");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return PrintResult.failed("Impressao interrompida");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            return PrintResult.failed(cause == null ? ex.getMessage() : cause.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
