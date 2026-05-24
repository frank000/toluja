package br.com.toluja.printagent.api.dto;

public record JobDelivery(
        String deliveryId,
        String printerId,
        String printerName,
        String channel,
        String destination,
        int copies
) {
}
