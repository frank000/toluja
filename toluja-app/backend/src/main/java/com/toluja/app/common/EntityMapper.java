package com.toluja.app.common;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.dto.OrderDtos;
import com.toluja.app.item.Item;
import com.toluja.app.order.Order;
import com.toluja.app.order.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public ItemDtos.ItemResponse toItemResponse(Item item) {
        return new ItemDtos.ItemResponse(item.getId(), item.getNome(), item.getPreco(), item.getAtivo());
    }

    public OrderDtos.OrderResponse toOrderResponse(Order order) {
        var itens = order.getItens().stream().map(this::toOrderItemResponse).toList();
        var user = new OrderDtos.UserSummary(order.getUser().getId(), order.getUser().getUsername(), order.getUser().getNomeExibicao());

        return new OrderDtos.OrderResponse(
                order.getId(),
                order.getCodigo(),
                order.getCriadoEm(),
                order.getStatus(),
                order.getTotal(),
                order.getObservacao(),
                user,
                itens
        );
    }

    public OrderDtos.OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return new OrderDtos.OrderItemResponse(
                orderItem.getId(),
                orderItem.getItem().getId(),
                orderItem.getNomeSnapshot(),
                orderItem.getPrecoSnapshot(),
                orderItem.getQuantidade(),
                orderItem.getSubtotal()
        );
    }
}
