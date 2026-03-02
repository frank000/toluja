package com.toluja.app.order;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.OrderDtos;
import com.toluja.app.item.Item;
import com.toluja.app.item.ItemRepository;
import com.toluja.app.print.PrintService;
import com.toluja.app.user.User;
import com.toluja.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final PrintService printService;
    private final Random random = new Random();

    public OrderDtos.OrderResponse criar(OrderDtos.CreateOrderRequest request, String username) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido sem itens");
        }

        User user = userRepository.findByUsernameAndAtivoTrue(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Order order = new Order();
        order.setCodigo(gerarCodigo());
        order.setCriadoEm(OffsetDateTime.now());
        order.setStatus("ABERTO");
        order.setObservacao(request.observacao());
        order.setTotal(BigDecimal.ZERO);
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDtos.CreateOrderItemRequest i : request.itens()) {
            Item item = itemRepository.findById(i.itemId())
                    .filter(Item::getAtivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setNomeSnapshot(item.getNome());
            orderItem.setPrecoSnapshot(item.getPreco());
            orderItem.setQuantidade(i.quantidade());
            BigDecimal subtotal = item.getPreco().multiply(BigDecimal.valueOf(i.quantidade()));
            orderItem.setSubtotal(subtotal);
            order.getItens().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);

        try {
            printService.printOrder(saved);
        } catch (Exception ex) {
            saved.setStatus("ERRO_IMPRESSAO");
            orderRepository.save(saved);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao imprimir o cupom em uma ou mais impressoras: " + ex.getMessage());
        }

        return mapper.toOrderResponse(saved);
    }

    public List<OrderDtos.OrderResponse> listar(String username) {
        User user = userRepository.findByUsernameAndAtivoTrue(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        List<Order> orders = "ADMIN".equals(user.getRole())
                ? orderRepository.findAll()
                : orderRepository.findByUserUsername(username);

        return orders.stream().map(mapper::toOrderResponse).toList();
    }

    private String gerarCodigo() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int tentativas = 0; tentativas < 20; tentativas++) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                builder.append(chars.charAt(random.nextInt(chars.length())));
            }
            String codigo = builder.toString();
            if (orderRepository.findByCodigo(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gerar código do pedido");
    }
}
