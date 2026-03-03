package com.toluja.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public class PrintAgentDtos {

    public record NextJobResponse(
            @NotBlank String jobId,
            @NotBlank String tenantId,
            @NotBlank String storeId,
            @NotBlank String deviceId,
            @NotBlank String orderId,
            @NotBlank String payloadType,
            @NotBlank String payloadBase64,
            @NotNull OffsetDateTime createdAt,
            @NotEmpty List<@Valid JobDelivery> deliveries
    ) {}

    public record JobDelivery(
            @NotBlank String deliveryId,
            @NotBlank String printerId,
            @NotBlank String printerName,
            @NotBlank String channel,
            @NotBlank String destination,
            @NotNull Integer copies
    ) {}

    public record AckRequest(
            @NotEmpty List<@Valid DeliveryAck> deliveries
    ) {}

    public record DeliveryAck(
            @NotBlank String deliveryId,
            @NotBlank String status,
            String errorMessage,
            @NotNull OffsetDateTime printedAt
    ) {}

    public record AckResponse(
            @NotBlank String jobId,
            @NotBlank String status,
            @NotNull Integer receivedDeliveries
    ) {}
}
