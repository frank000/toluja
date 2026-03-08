package com.toluja.app.common;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.dto.OrderDtos;
import com.toluja.app.item.Item;
import com.toluja.app.item.ItemImageStorageService;
import com.toluja.app.order.Order;
import com.toluja.app.order.OrderItem;
import com.toluja.app.order.OrderItemSubitem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityMapper {

    private final ItemImageStorageService itemImageStorageService;

    public ItemDtos.ItemResponse toItemResponse(Item item) {
        var categorias = item.getCategorias().stream().map(this::toSubitemCategoryResponse).toList();
        ItemDtos.SegmentResponse segmento = null;
        if (item.getSegment() != null) {
            segmento = new ItemDtos.SegmentResponse(
                    item.getSegment().getId(),
                    item.getSegment().getNome(),
                    item.getSegment().getCor(),
                    item.getSegment().getIcone()
            );
        }
        return new ItemDtos.ItemResponse(
                item.getId(),
                item.getNome(),
                item.getPreco(),
                itemImageStorageService.toPublicUrl(item.getImagePath()),
                item.getAtivo(),
                segmento,
                categorias
        );
    }

    public ItemDtos.SubitemCategoryResponse toSubitemCategoryResponse(com.toluja.app.item.SubitemCategory categoria) {
        var subitens = categoria.getSubitens().stream()
                .filter(subitem -> Boolean.TRUE.equals(subitem.getAtivo()))
                .map(subitem -> new ItemDtos.SubitemResponse(subitem.getId(), subitem.getNome(), subitem.getPreco()))
                .toList();
        return new ItemDtos.SubitemCategoryResponse(categoria.getId(), categoria.getNome(), subitens);
    }

    public OrderDtos.OrderResponse toOrderResponse(Order order) {
        var itens = order.getItens().stream().map(this::toOrderItemResponse).toList();
        var user = new OrderDtos.UserSummary(order.getUser().getId(), order.getUser().getUsername(), order.getUser().getNomeExibicao());

        return new OrderDtos.OrderResponse(
                order.getId(),
                order.getCodigo(),
                order.getSenhaChamada(),
                order.getCriadoEm(),
                order.getStatus(),
                order.getTotal(),
                order.getObservacao(),
                user,
                itens
        );
    }

    public OrderDtos.OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        var subitens = orderItem.getSubitens().stream().map(this::toOrderItemSubitemResponse).toList();
        return new OrderDtos.OrderItemResponse(
                orderItem.getId(),
                orderItem.getItem().getId(),
                orderItem.getNomeSnapshot(),
                orderItem.getPrecoSnapshot(),
                orderItem.getQuantidade(),
                orderItem.getSubtotal(),
                subitens
        );
    }

    public OrderDtos.OrderItemSubitemResponse toOrderItemSubitemResponse(OrderItemSubitem orderItemSubitem) {
        return new OrderDtos.OrderItemSubitemResponse(
                orderItemSubitem.getId(),
                orderItemSubitem.getSubitem().getId(),
                orderItemSubitem.getCategoriaNomeSnapshot(),
                orderItemSubitem.getNomeSnapshot(),
                orderItemSubitem.getPrecoSnapshot()
        );
    }
}
