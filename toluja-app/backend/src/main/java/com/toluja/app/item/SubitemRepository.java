package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubitemRepository extends JpaRepository<Subitem, Integer> {
    List<Subitem> findByIdInAndAtivoTrueAndTenantId(Collection<Integer> ids, String tenantId);
    Optional<Subitem> findByIdAndAtivoTrueAndTenantId(Integer id, String tenantId);
}
