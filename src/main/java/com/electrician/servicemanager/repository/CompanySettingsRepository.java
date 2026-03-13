package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
    Optional<CompanySettings> findByOwnerId(Long ownerId);
}