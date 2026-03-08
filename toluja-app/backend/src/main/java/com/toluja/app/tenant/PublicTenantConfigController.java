package com.toluja.app.tenant;

import com.toluja.app.dto.TenantConfigDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tenants/{tenantId}/configuracao")
@RequiredArgsConstructor
public class PublicTenantConfigController {

    private final TenantConfigService service;

    @GetMapping
    public TenantConfigDtos.TenantConfigResponse obter(@PathVariable String tenantId) {
        return service.obterConfiguracao(tenantId);
    }
}
