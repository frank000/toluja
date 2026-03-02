package com.toluja.app.order;

import com.toluja.app.dto.OrderDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public OrderDtos.OrderResponse criar(@Valid @RequestBody OrderDtos.CreateOrderRequest request, Authentication authentication) {
        return service.criar(request, authentication.getName());
    }

    @GetMapping
    public List<OrderDtos.OrderResponse> listar(Authentication authentication) {
        return service.listar(authentication.getName());
    }
}
