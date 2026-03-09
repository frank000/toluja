package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByAtivoTrueAndTenantId(String tenantId);
    Page<Item> findByAtivoTrueAndTenantId(String tenantId, Pageable pageable);
    Page<Item> findByAtivoTrueAndTenantIdAndNomeContainingIgnoreCase(String tenantId, String nome, Pageable pageable);
    List<Item> findByAtivoTrueAndTenantIdAndSegment_Id(String tenantId, Integer segmentId);
    Optional<Item> findByIdAndAtivoTrueAndTenantId(Integer id, String tenantId);
    boolean existsByTenantIdAndNomeIgnoreCaseAndAtivoTrue(String tenantId, String nome);
    boolean existsByTenantIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(String tenantId, String nome, Integer id);
}
