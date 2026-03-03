package com.toluja.app.superadmin;

import com.toluja.app.dto.SuperadminDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperadminController {

    private final SuperadminService service;

    @GetMapping("/tenants")
    public List<SuperadminDtos.TenantResponse> listarTenants(Authentication authentication) {
        service.ensureSuperadmin(authentication);
        return service.listarTenants();
    }

    @PostMapping("/tenants")
    public SuperadminDtos.TenantResponse criarTenant(@Valid @RequestBody SuperadminDtos.CreateTenantRequest request,
                                                      Authentication authentication) {
        service.ensureSuperadmin(authentication);
        return service.criarTenant(request);
    }

    @PostMapping("/users")
    public SuperadminDtos.UserResponse criarUsuario(@Valid @RequestBody SuperadminDtos.CreateUserRequest request,
                                                    Authentication authentication) {
        service.ensureSuperadmin(authentication);
        return service.criarUsuario(request);
    }
}
