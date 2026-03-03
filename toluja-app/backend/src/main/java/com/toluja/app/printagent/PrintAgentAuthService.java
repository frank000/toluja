package com.toluja.app.printagent;

import com.toluja.app.tenant.PrintKeyService;
import com.toluja.app.tenant.Tenant;
import com.toluja.app.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class PrintAgentAuthService {

    private final TenantRepository tenantRepository;
    private final PrintKeyService printKeyService;

    public PrintAgentAuthService(TenantRepository tenantRepository, PrintKeyService printKeyService) {
        this.tenantRepository = tenantRepository;
        this.printKeyService = printKeyService;
    }

    public Tenant authenticate(String printKey) {
        if (printKey == null || printKey.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Print key inválida");
        }
        String hash = printKeyService.hash(printKey.trim());
        return tenantRepository.findByPrintKeyHashAndAtivoTrue(hash)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Print key inválida"));
    }
}
