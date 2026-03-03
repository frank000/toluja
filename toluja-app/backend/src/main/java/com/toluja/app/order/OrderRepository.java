package com.toluja.app.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByCodigo(String codigo);
    Optional<Order> findByCodigoAndTenantId(String codigo, String tenantId);
    Optional<Order> findTopByTenantIdAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderBySenhaChamadaDesc(
            String tenantId,
            OffsetDateTime inicioDia,
            OffsetDateTime inicioProximoDia
    );
    List<Order> findByTenantId(String tenantId);
    List<Order> findByUserUsernameAndTenantId(String username, String tenantId);
}
