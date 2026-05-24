package br.com.toluja.printagent.api;

public class PrintAgentApiException extends Exception {
    private final boolean retryable;

    public PrintAgentApiException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public PrintAgentApiException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
