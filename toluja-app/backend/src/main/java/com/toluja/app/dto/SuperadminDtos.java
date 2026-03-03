package com.toluja.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SuperadminDtos {
    public record PublicTenantResponse(String tenantId, String nome) {}

    public record CreateTenantRequest(
            @NotBlank @Size(max = 64) String tenantId,
            @NotBlank @Size(max = 120) String nome
    ) {}

    public record TenantResponse(Integer id, String tenantId, String nome, Boolean ativo) {}

    public record CreateUserRequest(
            @NotBlank @Size(max = 64) String tenantId,
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 8, max = 120) String password,
            @NotBlank @Size(max = 120) String nomeExibicao,
            @NotBlank @Pattern(regexp = "ADMIN|ATENDENTE") String role
    ) {}

    public record UserResponse(Integer id, String tenantId, String username, String nomeExibicao, String role, Boolean ativo) {}
}
