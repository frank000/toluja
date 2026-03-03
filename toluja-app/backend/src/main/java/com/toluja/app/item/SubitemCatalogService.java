package com.toluja.app.item;

import com.toluja.app.common.EntityMapper;
import com.toluja.app.dto.ItemDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SubitemCatalogService {

    private final SubitemCategoryRepository subitemCategoryRepository;
    private final SubitemRepository subitemRepository;
    private final EntityMapper mapper;

    public List<ItemDtos.SubitemCategoryResponse> listarCategorias(String tenantId) {
        return subitemCategoryRepository.findByTenantId(tenantId)
                .stream()
                .map(mapper::toSubitemCategoryResponse)
                .toList();
    }

    public ItemDtos.SubitemCategoryResponse criarCategoria(ItemDtos.SubitemCategoryRequest request, String tenantId) {
        subitemCategoryRepository.findByNomeIgnoreCaseAndTenantId(request.nome(), tenantId)
                .ifPresent(categoria -> {
                    throw new ResponseStatusException(BAD_REQUEST, "Já existe uma categoria com esse nome");
                });

        SubitemCategory categoria = new SubitemCategory();
        categoria.setNome(request.nome().trim());
        categoria.setTenantId(tenantId);
        return mapper.toSubitemCategoryResponse(subitemCategoryRepository.save(categoria));
    }

    public ItemDtos.SubitemCategoryResponse criarSubitem(Integer categoriaId, ItemDtos.SubitemRequest request, String tenantId) {
        SubitemCategory categoria = subitemCategoryRepository.findByIdAndTenantId(categoriaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Categoria não encontrada"));

        boolean nomeDuplicado = categoria.getSubitens().stream()
                .anyMatch(subitem -> subitem.getNome().equalsIgnoreCase(request.nome().trim()));
        if (nomeDuplicado) {
            throw new ResponseStatusException(BAD_REQUEST, "Já existe subitem com esse nome na categoria");
        }

        Subitem subitem = new Subitem();
        subitem.setCategoria(categoria);
        subitem.setNome(request.nome().trim());
        subitem.setPreco(request.preco());
        subitem.setTenantId(tenantId);
        subitem.setAtivo(true);
        categoria.getSubitens().add(subitem);

        return mapper.toSubitemCategoryResponse(subitemCategoryRepository.save(categoria));
    }

    public ItemDtos.SubitemCategoryResponse excluirSubitem(Integer categoriaId, Integer subitemId, String tenantId) {
        SubitemCategory categoria = subitemCategoryRepository.findByIdAndTenantId(categoriaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Categoria não encontrada"));

        Subitem subitem = subitemRepository.findByIdAndAtivoTrueAndTenantId(subitemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Subitem não encontrado"));

        if (!subitem.getCategoria().getId().equals(categoria.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Subitem não pertence à categoria informada");
        }

        subitem.setAtivo(false);
        subitemRepository.save(subitem);
        return mapper.toSubitemCategoryResponse(categoria);
    }
}
