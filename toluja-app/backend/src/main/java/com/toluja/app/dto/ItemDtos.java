package com.toluja.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ItemDtos {
    public record ItemRequest(
            @NotBlank String nome,
            @NotNull @DecimalMin("0.01") BigDecimal preco,
            List<Integer> categoriaIds,
            Integer segmentoId
    ) {}
    public record ItemUpdateRequest(
            @NotBlank String nome,
            @NotNull @DecimalMin("0.01") BigDecimal preco
    ) {}
    public record ItemResponse(
            Integer id,
            String nome,
            BigDecimal preco,
            String imagemUrl,
            Boolean ativo,
            SegmentResponse segmento,
            List<SubitemCategoryResponse> categorias
    ) {}
    public record ItemPageResponse(
            List<ItemResponse> itens,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {}
    public record SegmentRequest(@NotBlank String nome, @NotBlank String cor, @NotBlank String icone) {}
    public record SegmentResponse(Integer id, String nome, String cor, String icone) {}

    public record SubitemCategoryRequest(@NotBlank String nome) {}
    public record SubitemRequest(@NotBlank String nome, @NotNull @DecimalMin("0.00") BigDecimal preco) {}
    public record SubitemResponse(Integer id, String nome, BigDecimal preco) {}
    public record SubitemCategoryResponse(Integer id, String nome, List<SubitemResponse> subitens) {}
}
