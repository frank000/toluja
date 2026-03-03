package com.toluja.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class AuthContext {

    private AuthContext() {}

    public static String tenantId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Autenticação inválida");
        }
        Object details = authentication.getDetails();
        if (details instanceof String tenantId && !tenantId.isBlank()) {
            return tenantId;
        }
        throw new ResponseStatusException(UNAUTHORIZED, "Tenant não encontrado no token");
    }
}
