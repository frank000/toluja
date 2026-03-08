package com.toluja.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class OrderDtos {
    public record CreateOrderRequest(
            String observacao,
            List<@Valid CreateOrderItemRequest> itens,
            @Size(max = 120) String clienteNome,
            @Size(max = 40) String clienteTelefone,
            @Pattern(regexp = "RETIRADA|ENTREGA") String tipoAtendimento
    ) {}
    public record CreateOrderItemRequest(@NotNull Integer itemId, @NotNull @Min(1) Integer quantidade, List<Integer> subitemIds) {}

    public record OrderResponse(Integer id, String codigo, Integer senhaChamada, OffsetDateTime criadoEm, String status, BigDecimal total,
                                String observacao, UserSummary user, List<OrderItemResponse> itens) {}
    public record UserSummary(Integer id, @NotBlank String username, String nomeExibicao) {}
    public record OrderItemResponse(Integer id, Integer itemId, String nomeSnapshot, BigDecimal precoSnapshot,
                                    Integer quantidade, BigDecimal subtotal, List<OrderItemSubitemResponse> subitens) {}
    public record OrderItemSubitemResponse(Integer id, Integer subitemId, String categoriaNomeSnapshot, String nomeSnapshot,
                                           BigDecimal precoSnapshot) {}
}
