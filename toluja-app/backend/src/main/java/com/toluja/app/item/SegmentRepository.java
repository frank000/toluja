package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Integer> {
    List<Segment> findByTenantIdOrderByOrdemAscIdAsc(String tenantId);
    List<Segment> findByTenantIdAndIdIn(String tenantId, List<Integer> ids);
    Optional<Segment> findByIdAndTenantId(Integer id, String tenantId);
    boolean existsByTenantIdAndNomeIgnoreCase(String tenantId, String nome);

    @Query("select coalesce(max(s.ordem), 0) from Segment s where s.tenantId = :tenantId")
    int findMaxOrdemByTenantId(String tenantId);
}
