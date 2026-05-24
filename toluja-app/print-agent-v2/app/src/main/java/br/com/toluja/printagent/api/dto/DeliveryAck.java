package br.com.toluja.printagent.api.dto;

import java.time.OffsetDateTime;

public record DeliveryAck(
        String deliveryId,
        String status,
        String errorMessage,
        OffsetDateTime printedAt
) {
    public static DeliveryAck success(String deliveryId) {
        return new DeliveryAck(deliveryId, "SUCCESS", null, OffsetDateTime.now());
    }

    public static DeliveryAck error(String deliveryId, String errorMessage) {
        return new DeliveryAck(deliveryId, "ERROR", errorMessage, OffsetDateTime.now());
    }
}
