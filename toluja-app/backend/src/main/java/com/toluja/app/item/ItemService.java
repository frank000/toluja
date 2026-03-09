package com.toluja.app.item;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    private final SegmentRepository segmentRepository;
    private final ItemImageStorageService itemImageStorageService;
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
        return criar(request, tenantId, null);
    }

    public ItemDtos.ItemResponse criar(ItemDtos.ItemRequest request, String tenantId, MultipartFile imagem) {
        String nome = request.nome().trim();
        if (itemRepository.existsByTenantIdAndNomeIgnoreCaseAndAtivoTrue(tenantId, nome)) {
            throw new ResponseStatusException(BAD_REQUEST, "Já existe item ativo com esse nome");
        }

        Item item = new Item();
        item.setNome(nome);
        item.setPreco(request.preco());
        item.setImagePath(itemImageStorageService.saveAsWebp(imagem, tenantId));
        item.setTenantId(tenantId);
        item.setAtivo(true);
        item.setSegment(buscarSegmento(request.segmentoId(), tenantId));
        item.setCategorias(buscarCategorias(request.categoriaIds(), tenantId));
        return mapper.toItemResponse(itemRepository.save(item));
    }

    public ItemDtos.ItemResponse atualizar(Integer itemId, ItemDtos.ItemUpdateRequest request, String tenantId) {
        return atualizar(itemId, request, tenantId, null);
    }

    public ItemDtos.ItemResponse atualizar(Integer itemId, ItemDtos.ItemUpdateRequest request, String tenantId, MultipartFile imagem) {
        Item item = itemRepository.findByIdAndAtivoTrueAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item não encontrado"));

        String nome = request.nome().trim();
        if (itemRepository.existsByTenantIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(tenantId, nome, itemId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Já existe item ativo com esse nome");
        }

        item.setNome(nome);
        item.setPreco(request.preco());
        if (request.categoriaIds() != null) {
            item.setCategorias(buscarCategorias(request.categoriaIds(), tenantId));
        }
        if (imagem != null && !imagem.isEmpty()) {
            String imagemAnterior = item.getImagePath();
            String novaImagem = itemImageStorageService.saveAsWebp(imagem, tenantId);
            item.setImagePath(novaImagem);
            itemImageStorageService.deleteIfExists(imagemAnterior);
        }
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

    private Segment buscarSegmento(Integer segmentoId, String tenantId) {
        if (segmentoId == null) {
            return null;
        }
        return segmentRepository.findByIdAndTenantId(segmentoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Segmento não encontrado"));
    }
}
