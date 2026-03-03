package com.toluja.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByAtivoTrueAndTenantId(String tenantId);
    Optional<Item> findByIdAndAtivoTrueAndTenantId(Integer id, String tenantId);
}
