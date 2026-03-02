package com.toluja.app.item;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final EntityMapper mapper;

    public List<ItemDtos.ItemResponse> listarAtivos() {
        return itemRepository.findByAtivoTrue().stream().map(mapper::toItemResponse).toList();
    }

    public ItemDtos.ItemResponse criar(ItemDtos.ItemRequest request) {
        Item item = new Item();
        item.setNome(request.nome());
        item.setPreco(request.preco());
        item.setAtivo(true);
        return mapper.toItemResponse(itemRepository.save(item));
    }
}
