package br.com.toluja.printagent.api.dto;

public record AckResponse(
        String jobId,
        String status,
        int receivedDeliveries
) {
}
