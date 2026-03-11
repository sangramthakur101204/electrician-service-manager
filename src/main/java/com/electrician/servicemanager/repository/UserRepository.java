package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    @Query("SELECT u FROM User u WHERE u.owner.id = :ownerId AND u.role = :role")
    List<User> findTechsByOwnerAndRole(@Param("ownerId") Long ownerId, @Param("role") String role);

    @Query("SELECT u FROM User u WHERE u.owner.id = :ownerId AND u.role = :role AND u.isActive = :isActive")
    List<User> findTechsByOwnerAndRoleAndActive(@Param("ownerId") Long ownerId, @Param("role") String role, @Param("isActive") Boolean isActive);
}