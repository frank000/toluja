package br.com.toluja.printagent.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record NextJobResponse(
        String jobId,
        String tenantId,
        String storeId,
        String deviceId,
        String orderId,
        String payloadType,
        String payloadBase64,
        OffsetDateTime createdAt,
        List<JobDelivery> deliveries
) {
    public NextJobResponse {
        deliveries = List.copyOf(deliveries);
    }
}
