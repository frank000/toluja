package br.com.toluja.printagent.api;

import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.AckResponse;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import br.com.toluja.printagent.config.AgentConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

public final class PrintAgentClient {
    private static final Logger LOGGER = Logger.getLogger(PrintAgentClient.class.getName());
    private static final String PRINT_KEY_HEADER = "X-Print-Key";

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final Duration timeout;

    public PrintAgentClient(AgentConfig config) {
        this.config = config;
        this.timeout = Duration.ofMillis(config.httpTimeoutMs());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    public Optional<NextJobResponse> fetchNextJob() throws PrintAgentApiException {
        return withRetry("buscar proximo job", this::fetchNextJobOnce);
    }

    public AckResponse sendAck(String jobId, AckRequest ack) throws PrintAgentApiException {
        return withRetry("enviar ACK do job " + jobId, () -> sendAckOnce(jobId, ack));
    }

    private Optional<NextJobResponse> fetchNextJobOnce() throws PrintAgentApiException {
        String encodedDeviceId = URLEncoder.encode(config.deviceId(), StandardCharsets.UTF_8);
        URI uri = URI.create(apiBaseUrl() + "/api/print-agent/jobs/next?deviceId=" + encodedDeviceId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET()
                .header(PRINT_KEY_HEADER, config.printKey())
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 204) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw httpStatusError("next job", response);
        }
        return Optional.of(PrintAgentJson.parseNextJob(response.body()));
    }

    private AckResponse sendAckOnce(String jobId, AckRequest ack) throws PrintAgentApiException {
        URI uri = URI.create(apiBaseUrl() + "/api/print-agent/jobs/" + urlPath(jobId) + "/ack");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(PrintAgentJson.writeAckRequest(ack)))
                .header(PRINT_KEY_HEADER, config.printKey())
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw httpStatusError("ack", response);
        }
        return PrintAgentJson.parseAckResponse(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) throws PrintAgentApiException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new PrintAgentApiException("Falha de rede: " + ex.getMessage(), ex, true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PrintAgentApiException("Operacao interrompida", ex, false);
        }
    }

    private PrintAgentApiException httpStatusError(String operation, HttpResponse<String> response) {
        int status = response.statusCode();
        boolean retryable = status >= 500 || status == 408 || status == 429;
        String body = response.body() == null ? "" : response.body();
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        return new PrintAgentApiException(
                "Falha HTTP em " + operation + ": status=" + status + " body=" + body,
                retryable
        );
    }

    private <T> T withRetry(String operation, ApiCall<T> call) throws PrintAgentApiException {
        int attempts = Math.max(1, config.apiRetryAttempts());
        PrintAgentApiException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return call.run();
            } catch (PrintAgentApiException ex) {
                last = ex;
                if (!ex.retryable() || attempt == attempts) {
                    throw ex;
                }
                long waitMs = (long) config.apiRetryBackoffMs() * attempt;
                LOGGER.warning(operation + " falhou, tentativa " + attempt + "/" + attempts
                        + ". Nova tentativa em " + waitMs + "ms: " + ex.getMessage());
                sleep(waitMs);
            }
        }
        throw last == null
                ? new PrintAgentApiException("Falha desconhecida ao " + operation, false)
                : last;
    }

    private void sleep(long waitMs) throws PrintAgentApiException {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PrintAgentApiException("Operacao interrompida durante retry", ex, false);
        }
    }

    private String apiBaseUrl() {
        String value = config.apiBaseUrl();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String urlPath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T run() throws PrintAgentApiException;
    }
}
