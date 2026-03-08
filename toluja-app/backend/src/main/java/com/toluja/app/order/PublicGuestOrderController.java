package com.toluja.app.order;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.dto.OrderDtos;
import com.toluja.app.item.ItemService;
import com.toluja.app.item.SegmentService;
import com.toluja.app.tenant.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/tenants/{tenantId}")
@RequiredArgsConstructor
public class PublicGuestOrderController {

    private final ItemService itemService;
    private final SegmentService segmentService;
    private final OrderService orderService;
    private final TenantRepository tenantRepository;

    @GetMapping("/itens")
    public ItemDtos.ItemPageResponse listarItens(
            @PathVariable String tenantId,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        validarTenantAtivo(tenantId);
        return itemService.listarAtivos(tenantId, nome, page, size);
    }

    @GetMapping("/segmentos")
    public List<ItemDtos.SegmentResponse> listarSegmentos(@PathVariable String tenantId) {
        validarTenantAtivo(tenantId);
        return segmentService.listar(tenantId);
    }

    @PostMapping("/pedidos")
    public OrderDtos.OrderResponse criarPedidoGuest(@PathVariable String tenantId,
                                                    @Valid @RequestBody OrderDtos.CreateOrderRequest request) {
        validarTenantAtivo(tenantId);
        return orderService.criarGuest(request, tenantId);
    }

    private void validarTenantAtivo(String tenantId) {
        if (!tenantRepository.existsByTenantIdAndAtivoTrue(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado");
        }
    }
}
