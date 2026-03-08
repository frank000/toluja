package com.toluja.app.tenant;

import com.toluja.app.dto.TenantConfigDtos;
import com.toluja.app.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracao/tenant")
@RequiredArgsConstructor
public class TenantConfigController {

    private final TenantConfigService service;

    @GetMapping
    public TenantConfigDtos.TenantConfigResponse obter(Authentication authentication) {
        return service.obterConfiguracao(AuthContext.tenantId(authentication));
    }

    @PutMapping
    public TenantConfigDtos.TenantConfigResponse atualizar(@Valid @RequestBody TenantConfigDtos.UpdateTenantConfigRequest request,
                                                           Authentication authentication) {
        return service.atualizarConfiguracao(AuthContext.tenantId(authentication), request);
    }
}
