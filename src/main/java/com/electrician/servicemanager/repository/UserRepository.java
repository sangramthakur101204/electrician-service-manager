package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobile(String mobile);
    List<User> findByOwnerIdAndRole(Long ownerId, String role);
    List<User> findByOwnerIdAndRoleAndIsActive(Long ownerId, String role, Boolean isActive);
    boolean existsByMobile(String mobile);
}
