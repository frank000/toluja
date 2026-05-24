package br.com.toluja.printagent.config;

public class ConfigValidationException extends Exception {
    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
