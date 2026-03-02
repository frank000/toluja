package com.toluja.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ItemDtos {
    public record ItemRequest(@NotBlank String nome, @NotNull @DecimalMin("0.01") BigDecimal preco) {}
    public record ItemResponse(Integer id, String nome, BigDecimal preco, Boolean ativo) {}
}
