package com.toluja.app.item;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final SubitemCategoryRepository subitemCategoryRepository;
    private final EntityMapper mapper;

    public List<ItemDtos.ItemResponse> listarAtivos(String tenantId) {
        return itemRepository.findByAtivoTrueAndTenantId(tenantId).stream().map(mapper::toItemResponse).toList();
    }

    public ItemDtos.ItemResponse criar(ItemDtos.ItemRequest request, String tenantId) {
        Item item = new Item();
        item.setNome(request.nome());
        item.setPreco(request.preco());
        item.setTenantId(tenantId);
        item.setAtivo(true);
        item.setCategorias(buscarCategorias(request.categoriaIds(), tenantId));
        return mapper.toItemResponse(itemRepository.save(item));
    }

    public ItemDtos.ItemResponse atualizar(Integer itemId, ItemDtos.ItemUpdateRequest request, String tenantId) {
        Item item = itemRepository.findByIdAndAtivoTrueAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item não encontrado"));

        item.setNome(request.nome().trim());
        item.setPreco(request.preco());
        return mapper.toItemResponse(itemRepository.save(item));
    }

    public void excluir(Integer itemId, String tenantId) {
        Item item = itemRepository.findByIdAndAtivoTrueAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item não encontrado"));

        item.setAtivo(false);
        itemRepository.save(item);
    }

    private Set<SubitemCategory> buscarCategorias(List<Integer> categoriaIds, String tenantId) {
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            return new HashSet<>();
        }
        List<SubitemCategory> categorias = subitemCategoryRepository.findByIdInAndTenantId(categoriaIds, tenantId);
        if (categorias.size() != categoriaIds.stream().distinct().count()) {
            throw new ResponseStatusException(NOT_FOUND, "Uma ou mais categorias de subitens não foram encontradas");
        }
        if (categorias.stream().anyMatch(categoria -> categoria.getSubitens().isEmpty())) {
            throw new ResponseStatusException(BAD_REQUEST, "Categoria sem subitens não pode ser associada ao item");
        }
        return new HashSet<>(categorias);
    }
}
