package br.com.toluja.printagent.print;

public record PrintResult(
        boolean success,
        String errorMessage
) {
    public static PrintResult ok() {
        return new PrintResult(true, null);
    }

    public static PrintResult failed(String errorMessage) {
        return new PrintResult(false, errorMessage);
    }
}
