package br.com.toluja.printagent.print;

public class PrintBackendException extends Exception {
    public PrintBackendException(String message) {
        super(message);
    }

    public PrintBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
