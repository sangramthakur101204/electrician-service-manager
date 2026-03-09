package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.TechnicianLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianLocationRepository extends JpaRepository<TechnicianLocation, Long> {
}
