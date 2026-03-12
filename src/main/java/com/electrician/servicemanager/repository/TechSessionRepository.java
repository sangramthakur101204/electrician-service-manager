package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.TechSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TechSessionRepository extends JpaRepository<TechSession, Long> {

    // Latest open session (not yet ended)
    @Query("SELECT s FROM TechSession s WHERE s.technicianId = :techId AND s.endTime IS NULL ORDER BY s.startTime DESC")
    Optional<TechSession> findOpenSession(@Param("techId") Long techId);

    // All sessions for a technician
    @Query("SELECT s FROM TechSession s WHERE s.technicianId = :techId ORDER BY s.startTime DESC")
    List<TechSession> findByTechnicianId(@Param("techId") Long techId);

    // Sessions for a technician on a specific date
    @Query("SELECT s FROM TechSession s WHERE s.technicianId = :techId AND s.sessionDate = :date ORDER BY s.startTime DESC")
    List<TechSession> findByTechIdAndDate(@Param("techId") Long techId, @Param("date") LocalDate date);

    // Sessions for a technician in a date range (for monthly history)
    @Query("SELECT s FROM TechSession s WHERE s.technicianId = :techId AND s.sessionDate >= :from AND s.sessionDate <= :to ORDER BY s.startTime DESC")
    List<TechSession> findByTechIdAndDateRange(@Param("techId") Long techId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // All sessions in a date range (owner sees all techs)
    @Query("SELECT s FROM TechSession s WHERE s.sessionDate >= :from AND s.sessionDate <= :to ORDER BY s.startTime DESC")
    List<TechSession> findAllInDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}