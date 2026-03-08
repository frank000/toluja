package com.toluja.app.tenant;

import com.toluja.app.dto.TenantConfigDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantConfigService {

    private final TenantRepository tenantRepository;

    public TenantConfigDtos.TenantConfigResponse obterConfiguracao(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado"));
        return new TenantConfigDtos.TenantConfigResponse(
                Boolean.TRUE.equals(tenant.getEntregaAtiva()),
                tenant.getInfoTelaResumo(),
                tenant.getWhatsappNumero()
        );
    }

    public TenantConfigDtos.TenantConfigResponse atualizarConfiguracao(String tenantId, TenantConfigDtos.UpdateTenantConfigRequest request) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado"));
        tenant.setEntregaAtiva(request.entregaAtiva());
        tenant.setInfoTelaResumo(request.informacaoTelaResumo() == null ? null : request.informacaoTelaResumo().trim());
        tenant.setWhatsappNumero(normalizarWhatsapp(request.whatsappNumero()));
        Tenant salvo = tenantRepository.save(tenant);
        return new TenantConfigDtos.TenantConfigResponse(
                Boolean.TRUE.equals(salvo.getEntregaAtiva()),
                salvo.getInfoTelaResumo(),
                salvo.getWhatsappNumero()
        );
    }

    private String normalizarWhatsapp(String whatsappNumero) {
        if (whatsappNumero == null) {
            return null;
        }
        String digits = whatsappNumero.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
