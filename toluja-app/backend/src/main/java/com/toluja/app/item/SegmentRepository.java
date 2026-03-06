package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Integer> {
    List<Segment> findByTenantIdOrderByNomeAsc(String tenantId);
    Optional<Segment> findByIdAndTenantId(Integer id, String tenantId);
    boolean existsByTenantIdAndNomeIgnoreCase(String tenantId, String nome);
}
