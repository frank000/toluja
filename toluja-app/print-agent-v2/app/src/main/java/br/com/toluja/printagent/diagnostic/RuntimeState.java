package br.com.toluja.printagent.diagnostic;

public record RuntimeState(
        String updatedAt,
        String lastJobId,
        String lastOrderId,
        String lastJobAt,
        String lastAckStatus,
        String lastAckAt,
        String lastErrorType,
        String lastErrorMessage,
        String lastErrorAt
) {
    public static RuntimeState empty() {
        return new RuntimeState(null, null, null, null, null, null, null, null, null);
    }
}
