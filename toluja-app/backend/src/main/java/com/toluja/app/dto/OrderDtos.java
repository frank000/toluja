package com.toluja.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class OrderDtos {
    public record CreateOrderRequest(String observacao, List<@Valid CreateOrderItemRequest> itens) {}
    public record CreateOrderItemRequest(@NotNull Long itemId, @NotNull @Min(1) Integer quantidade) {}

    public record OrderResponse(Long id, String codigo, OffsetDateTime criadoEm, String status, BigDecimal total,
                                String observacao, UserSummary user, List<OrderItemResponse> itens) {}
    public record UserSummary(Long id, @NotBlank String username, String nomeExibicao) {}
    public record OrderItemResponse(Long id, Long itemId, String nomeSnapshot, BigDecimal precoSnapshot,
                                    Integer quantidade, BigDecimal subtotal) {}
}
