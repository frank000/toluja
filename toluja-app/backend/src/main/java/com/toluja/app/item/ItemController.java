package com.toluja.app.item;

import com.toluja.app.dto.ItemDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public List<ItemDtos.ItemResponse> listar() {
        return service.listarAtivos();
    }

    @PostMapping
    public ItemDtos.ItemResponse criar(@Valid @RequestBody ItemDtos.ItemRequest request) {
        return service.criar(request);
    }
}
