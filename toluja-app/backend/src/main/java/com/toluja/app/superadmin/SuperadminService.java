package com.toluja.app.superadmin;

import com.toluja.app.dto.SuperadminDtos;
import com.toluja.app.tenant.PrintKeyService;
import com.toluja.app.tenant.Tenant;
import com.toluja.app.tenant.TenantRepository;
import com.toluja.app.user.User;
import com.toluja.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperadminService {

    private final TenantRepository tenantRepository;
    private final PrintKeyService printKeyService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void ensureSuperadmin(Authentication authentication) {
        if (authentication == null || !"superadmin".equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido somente para superadmin");
        }
    }

    public List<SuperadminDtos.PublicTenantResponse> listarTenantsPublicos() {
        return tenantRepository.findByAtivoTrueOrderByTenantIdAsc().stream()
                .map(t -> new SuperadminDtos.PublicTenantResponse(t.getTenantId(), t.getNome()))
                .toList();
    }

    public List<SuperadminDtos.TenantResponse> listarTenants() {
        return tenantRepository.findByAtivoTrueOrderByTenantIdAsc().stream()
                .map(t -> new SuperadminDtos.TenantResponse(
                        t.getId(),
                        t.getTenantId(),
                        t.getNome(),
                        t.getAtivo(),
                        t.getPrintKeyHash() != null && !t.getPrintKeyHash().isBlank()
                ))
                .toList();
    }

    public SuperadminDtos.CreateTenantResponse criarTenant(SuperadminDtos.CreateTenantRequest request) {
        String tenantId = normalizarTenantId(request.tenantId());
        if (tenantRepository.existsByTenantId(tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant já cadastrado");
        }
        String printKey = generateUniquePrintKey();
        String printKeyHash = printKeyService.hash(printKey);

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setNome(request.nome().trim());
        tenant.setPrintKeyHash(printKeyHash);
        tenant.setAtivo(true);
        tenant.setEntregaAtiva(false);
        Tenant salvo = tenantRepository.save(tenant);

        return new SuperadminDtos.CreateTenantResponse(
                salvo.getId(),
                salvo.getTenantId(),
                salvo.getNome(),
                salvo.getAtivo(),
                true,
                printKey
        );
    }

    public SuperadminDtos.UserResponse criarUsuario(SuperadminDtos.CreateUserRequest request) {
        String tenantId = normalizarTenantId(request.tenantId());
        if (!tenantRepository.existsByTenantIdAndAtivoTrue(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não encontrado");
        }

        String username = request.username().trim();
        if (userRepository.existsByUsernameAndTenantId(username, tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já cadastrado neste tenant");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setNomeExibicao(request.nomeExibicao().trim());
        user.setRole(request.role().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAtivo(true);
        user.setDeveTrocarSenha(true);

        User salvo = userRepository.save(user);
        return new SuperadminDtos.UserResponse(
                salvo.getId(),
                salvo.getTenantId(),
                salvo.getUsername(),
                salvo.getNomeExibicao(),
                salvo.getRole(),
                salvo.getAtivo()
        );
    }

    public void garantirTenantPadrao() {
        var existing = tenantRepository.findByTenantId("default");
        if (existing.isPresent()) {
            Tenant tenant = existing.get();
            if (tenant.getPrintKeyHash() == null || tenant.getPrintKeyHash().isBlank()) {
                tenant.setPrintKeyHash(printKeyService.hash("dev-print-key-default"));
            }
            if (tenant.getEntregaAtiva() == null) {
                tenant.setEntregaAtiva(false);
            }
            tenantRepository.save(tenant);
            return;
        }
        Tenant tenant = new Tenant();
        tenant.setTenantId("default");
        tenant.setNome("Tenant Padrão");
        tenant.setPrintKeyHash(printKeyService.hash("dev-print-key-default"));
        tenant.setAtivo(true);
        tenant.setEntregaAtiva(false);
        tenantRepository.save(tenant);
    }

    private String normalizarTenantId(String tenantId) {
        return tenantId == null ? "" : tenantId.trim().toLowerCase();
    }

    private String generateUniquePrintKey() {
        for (int i = 0; i < 10; i++) {
            String candidate = printKeyService.generate();
            String hash = printKeyService.hash(candidate);
            if (!tenantRepository.existsByPrintKeyHash(hash)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Falha ao gerar print key única");
    }
}
