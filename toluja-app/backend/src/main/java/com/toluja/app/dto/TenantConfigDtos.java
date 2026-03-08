package com.toluja.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TenantConfigDtos {
    public record TenantConfigResponse(Boolean entregaAtiva, String informacaoTelaResumo, String whatsappNumero) {}
    public record UpdateTenantConfigRequest(
            @NotNull Boolean entregaAtiva,
            @Size(max = 500) String informacaoTelaResumo,
            @Pattern(regexp = "^55\\d{10,11}$|^$", message = "WhatsApp deve estar no formato 55 + DDD + telefone")
            String whatsappNumero
    ) {}
}
