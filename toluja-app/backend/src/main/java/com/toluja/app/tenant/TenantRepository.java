package com.toluja.app.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
    List<Tenant> findByAtivoTrueOrderByTenantIdAsc();
    Optional<Tenant> findByTenantId(String tenantId);
    Optional<Tenant> findByPrintKeyHashAndAtivoTrue(String printKeyHash);
    boolean existsByPrintKeyHash(String printKeyHash);
    boolean existsByTenantId(String tenantId);
    boolean existsByTenantIdAndAtivoTrue(String tenantId);
}
