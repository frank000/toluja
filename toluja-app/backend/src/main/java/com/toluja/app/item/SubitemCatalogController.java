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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subitens/categorias")
@RequiredArgsConstructor
public class SubitemCatalogController {

    private final SubitemCatalogService service;

    @GetMapping
    public List<ItemDtos.SubitemCategoryResponse> listarCategorias(Authentication authentication) {
        return service.listarCategorias(AuthContext.tenantId(authentication));
    }

    @PostMapping
    public ItemDtos.SubitemCategoryResponse criarCategoria(@Valid @RequestBody ItemDtos.SubitemCategoryRequest request,
                                                           Authentication authentication) {
        return service.criarCategoria(request, AuthContext.tenantId(authentication));
    }

    @PostMapping("/{categoriaId}/subitens")
    public ItemDtos.SubitemCategoryResponse criarSubitem(
            @PathVariable Integer categoriaId,
            @Valid @RequestBody ItemDtos.SubitemRequest request,
            Authentication authentication
    ) {
        return service.criarSubitem(categoriaId, request, AuthContext.tenantId(authentication));
    }

    @DeleteMapping("/{categoriaId}/subitens/{subitemId}")
    public ItemDtos.SubitemCategoryResponse excluirSubitem(
            @PathVariable Integer categoriaId,
            @PathVariable Integer subitemId,
            Authentication authentication
    ) {
        return service.excluirSubitem(categoriaId, subitemId, AuthContext.tenantId(authentication));
    }
}
