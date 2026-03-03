package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubitemCategoryRepository extends JpaRepository<SubitemCategory, Integer> {
    Optional<SubitemCategory> findByNomeIgnoreCaseAndTenantId(String nome, String tenantId);
    Optional<SubitemCategory> findByIdAndTenantId(Integer id, String tenantId);
    List<SubitemCategory> findByIdInAndTenantId(Collection<Integer> ids, String tenantId);
    List<SubitemCategory> findByTenantId(String tenantId);
}
