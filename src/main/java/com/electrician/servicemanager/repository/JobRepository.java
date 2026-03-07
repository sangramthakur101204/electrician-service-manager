package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    // Owner ke saare jobs
    @Query("SELECT j FROM Job j WHERE j.owner.id = :ownerId ORDER BY j.createdAt DESC")
    List<Job> findByOwnerId(@Param("ownerId") Long ownerId);

    // Owner ke jobs by status
    @Query("SELECT j FROM Job j WHERE j.owner.id = :ownerId AND j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);

    // Technician ke assigned jobs
    @Query("SELECT j FROM Job j WHERE j.technician.id = :techId ORDER BY j.createdAt DESC")
    List<Job> findByTechnicianId(@Param("techId") Long techId);

    // Technician ke pending jobs
    @Query("SELECT j FROM Job j WHERE j.technician.id = :techId AND j.status NOT IN ('DONE','CANCELLED') ORDER BY j.createdAt DESC")
    List<Job> findActivByTechnicianId(@Param("techId") Long techId);

    // Owner ke active jobs count
    @Query("SELECT COUNT(j) FROM Job j WHERE j.owner.id = :ownerId AND j.status NOT IN ('DONE','CANCELLED')")
    long countActiveByOwnerId(@Param("ownerId") Long ownerId);
}
