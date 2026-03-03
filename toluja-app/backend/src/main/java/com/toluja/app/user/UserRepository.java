package com.toluja.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsernameAndTenantIdAndAtivoTrue(String username, String tenantId);
    Optional<User> findByUsernameAndTenantId(String username, String tenantId);
    boolean existsByUsernameAndTenantId(String username, String tenantId);
}
