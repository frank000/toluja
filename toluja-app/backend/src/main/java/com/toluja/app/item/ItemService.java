package com.toluja.app.item;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public ItemDtos.ItemPageResponse listarAtivos(String tenantId, String nome, Integer page, Integer size) {
        int pagina = (page == null || page < 0) ? 0 : page;
        int tamanho = (size == null || size < 1) ? 10 : Math.min(size, 100);
        String filtroNome = nome == null ? "" : nome.trim();

        Pageable pageable = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.ASC, "nome").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<Item> itensPage = filtroNome.isBlank()
                ? itemRepository.findByAtivoTrueAndTenantId(tenantId, pageable)
                : itemRepository.findByAtivoTrueAndTenantIdAndNomeContainingIgnoreCase(tenantId, filtroNome, pageable);

        List<ItemDtos.ItemResponse> itens = itensPage.getContent().stream().map(mapper::toItemResponse).toList();
        return new ItemDtos.ItemPageResponse(
                itens,
                itensPage.getNumber(),
                itensPage.getSize(),
                itensPage.getTotalElements(),
                itensPage.getTotalPages(),
                itensPage.isFirst(),
                itensPage.isLast()
        );
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
