package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import com.toluja.app.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @GetMapping
    public ItemDtos.ItemPageResponse listar(
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication
    ) {
        return service.listarAtivos(AuthContext.tenantId(authentication), nome, page, size);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ItemDtos.ItemResponse criar(@Valid @RequestBody ItemDtos.ItemRequest request, Authentication authentication) {
        return service.criar(request, AuthContext.tenantId(authentication));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemDtos.ItemResponse criarComImagem(
            @Valid @RequestPart("payload") ItemDtos.ItemRequest request,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem,
            Authentication authentication
    ) {
        return service.criar(request, AuthContext.tenantId(authentication), imagem);
    }

    @PutMapping(value = "/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ItemDtos.ItemResponse atualizar(
            @PathVariable Integer itemId,
            @Valid @RequestBody ItemDtos.ItemUpdateRequest request,
            Authentication authentication
    ) {
        return service.atualizar(itemId, request, AuthContext.tenantId(authentication));
    }

    @PutMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemDtos.ItemResponse atualizarComImagem(
            @PathVariable Integer itemId,
            @Valid @RequestPart("payload") ItemDtos.ItemUpdateRequest request,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem,
            Authentication authentication
    ) {
        return service.atualizar(itemId, request, AuthContext.tenantId(authentication), imagem);
    }

    @DeleteMapping("/{itemId}")
    public void excluir(@PathVariable Integer itemId, Authentication authentication) {
        service.excluir(itemId, AuthContext.tenantId(authentication));
    }
}
