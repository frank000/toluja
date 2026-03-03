package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @GetMapping
    public List<ItemDtos.ItemResponse> listar(Authentication authentication) {
        return service.listarAtivos(AuthContext.tenantId(authentication));
    }

    @PostMapping
    public ItemDtos.ItemResponse criar(@Valid @RequestBody ItemDtos.ItemRequest request, Authentication authentication) {
        return service.criar(request, AuthContext.tenantId(authentication));
    }

    @PutMapping("/{itemId}")
    public ItemDtos.ItemResponse atualizar(
            @PathVariable Integer itemId,
            @Valid @RequestBody ItemDtos.ItemUpdateRequest request,
            Authentication authentication
    ) {
        return service.atualizar(itemId, request, AuthContext.tenantId(authentication));
    }

    @DeleteMapping("/{itemId}")
    public void excluir(@PathVariable Integer itemId, Authentication authentication) {
        service.excluir(itemId, AuthContext.tenantId(authentication));
    }
}
