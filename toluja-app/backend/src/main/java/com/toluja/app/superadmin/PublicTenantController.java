package com.toluja.app.superadmin;

import com.toluja.app.dto.SuperadminDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/tenants")
@RequiredArgsConstructor
public class PublicTenantController {

    private final SuperadminService service;

    @GetMapping
    public List<SuperadminDtos.PublicTenantResponse> listar() {
        return service.listarTenantsPublicos();
    }
}
