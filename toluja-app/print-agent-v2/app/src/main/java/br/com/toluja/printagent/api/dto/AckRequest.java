package br.com.toluja.printagent.api.dto;

import java.util.List;

public record AckRequest(List<DeliveryAck> deliveries) {
    public AckRequest {
        deliveries = List.copyOf(deliveries);
    }
}
