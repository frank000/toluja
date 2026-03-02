package com.toluja.app.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, UserInfo user) {}
    public record UserInfo(Long id, String username, String nomeExibicao, String role, Boolean deveTrocarSenha) {}
    public record ChangePasswordRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {}
}
