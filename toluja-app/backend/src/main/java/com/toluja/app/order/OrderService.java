package com.toluja.app.order;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.OrderDtos;
import com.toluja.app.item.Item;
import com.toluja.app.item.ItemRepository;
import com.toluja.app.item.Subitem;
import com.toluja.app.item.SubitemRepository;
import com.toluja.app.print.PrintService;
import com.toluja.app.tenant.TenantRepository;
import com.toluja.app.user.User;
import com.toluja.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final SubitemRepository subitemRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper mapper;
    private final PrintService printService;
    private final Random random = new Random();
    private static final String GUEST_USERNAME = "__guest__";

    public OrderDtos.OrderResponse criar(OrderDtos.CreateOrderRequest request, String username, String tenantId) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido sem itens");
        }
        User user = buscarUsuarioAtivo(username, tenantId);
        return criarInterno(request, user, tenantId);
    }

    public OrderDtos.OrderResponse criarGuest(OrderDtos.CreateOrderRequest request, String tenantId) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido sem itens");
        }
        validarTenantAtivo(tenantId);
        User guestUser = obterOuCriarUsuarioGuest(tenantId);
        return criarInterno(request, guestUser, tenantId);
    }

    public void reimprimir(Integer orderId, String username, String tenantId) {
        buscarUsuarioAtivo(username, tenantId);
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        try {
            printService.printOrder(order);
        } catch (Exception ex) {
            order.setStatus("ERRO_IMPRESSAO");
            orderRepository.save(order);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao imprimir o cupom em uma ou mais impressoras: " + ex.getMessage());
        }
    }

    private OrderDtos.OrderResponse criarInterno(OrderDtos.CreateOrderRequest request, User user, String tenantId) {

        Order order = new Order();
        OffsetDateTime agora = OffsetDateTime.now();
        order.setCodigo(gerarCodigo(tenantId));
        order.setSenhaChamada(gerarProximaSenhaChamada(agora, tenantId));
        order.setCriadoEm(agora);
        order.setStatus("ABERTO");
        order.setTenantId(tenantId);
        order.setObservacao(request.observacao());
        order.setTotal(BigDecimal.ZERO);
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDtos.CreateOrderItemRequest i : request.itens()) {
            Item item = itemRepository.findByIdAndAtivoTrueAndTenantId(i.itemId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setNomeSnapshot(item.getNome());
            orderItem.setPrecoSnapshot(item.getPreco());
            orderItem.setQuantidade(i.quantidade());

            Set<Integer> categoriaPermitidas = item.getCategorias().stream().map(categoria -> categoria.getId()).collect(Collectors.toSet());
            Set<Integer> subitemIds = i.subitemIds() == null ? Set.of() : new HashSet<>(i.subitemIds());
            List<Subitem> subitens = subitemIds.isEmpty() ? List.of() : subitemRepository.findByIdInAndAtivoTrueAndTenantId(subitemIds, tenantId);
            if (subitens.size() != subitemIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais subitens são inválidos");
            }
            if (subitens.stream().anyMatch(subitem -> !categoriaPermitidas.contains(subitem.getCategoria().getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subitem não permitido para o item selecionado");
            }

            BigDecimal adicionais = BigDecimal.ZERO;
            for (Subitem subitem : subitens) {
                OrderItemSubitem orderItemSubitem = new OrderItemSubitem();
                orderItemSubitem.setOrderItem(orderItem);
                orderItemSubitem.setSubitem(subitem);
                orderItemSubitem.setCategoriaNomeSnapshot(subitem.getCategoria().getNome());
                orderItemSubitem.setNomeSnapshot(subitem.getNome());
                orderItemSubitem.setPrecoSnapshot(subitem.getPreco());
                orderItem.getSubitens().add(orderItemSubitem);
                adicionais = adicionais.add(subitem.getPreco());
            }

            BigDecimal subtotal = item.getPreco()
                    .add(adicionais)
                    .multiply(BigDecimal.valueOf(i.quantidade()));
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

    public List<OrderDtos.OrderResponse> listar(String username, String tenantId) {
        User user = buscarUsuarioAtivo(username, tenantId);

        List<Order> orders = "ADMIN".equals(user.getRole())
                ? orderRepository.findByTenantId(tenantId)
                : orderRepository.findByUserUsernameAndTenantId(username, tenantId);

        return orders.stream().map(mapper::toOrderResponse).toList();
    }

    private String gerarCodigo(String tenantId) {
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

    private Integer gerarProximaSenhaChamada(OffsetDateTime referencia, String tenantId) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate hoje = referencia.atZoneSameInstant(zoneId).toLocalDate();
        OffsetDateTime inicioDia = hoje.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime inicioProximoDia = hoje.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        return orderRepository.findTopByTenantIdAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderBySenhaChamadaDesc(
                        tenantId,
                        inicioDia,
                        inicioProximoDia
                )
                .map(order -> order.getSenhaChamada() + 1)
                .orElse(1);
    }

    private User buscarUsuarioAtivo(String username, String tenantId) {
        return userRepository.findByUsernameAndTenantIdAndAtivoTrue(username, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private void validarTenantAtivo(String tenantId) {
        if (!tenantRepository.existsByTenantIdAndAtivoTrue(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado");
        }
    }

    private User obterOuCriarUsuarioGuest(String tenantId) {
        return userRepository.findByUsernameAndTenantIdAndAtivoTrue(GUEST_USERNAME, tenantId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setTenantId(tenantId);
                    user.setUsername(GUEST_USERNAME);
                    user.setNomeExibicao("Guest");
                    user.setRole("ATENDENTE");
                    user.setPasswordHash(passwordEncoder.encode("guest-" + tenantId + "-" + System.nanoTime()));
                    user.setAtivo(true);
                    user.setDeveTrocarSenha(false);
                    return userRepository.save(user);
                });
    }
}
