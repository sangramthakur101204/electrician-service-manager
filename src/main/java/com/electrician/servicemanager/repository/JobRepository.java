package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j WHERE j.owner.id = :ownerId ORDER BY j.createdAt DESC")
    List<Job> findJobsByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT j FROM Job j WHERE j.owner.id = :ownerId AND j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findJobsByOwnerAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);

    @Query("SELECT j FROM Job j WHERE j.technician.id = :techId ORDER BY j.createdAt DESC")
    List<Job> findJobsByTechnician(@Param("techId") Long techId);

    @Query("SELECT j FROM Job j WHERE j.technician.id = :techId AND j.status NOT IN ('DONE','CANCELLED') ORDER BY j.createdAt DESC")
    List<Job> findActiveJobsByTechnician(@Param("techId") Long techId);

    @Query("SELECT j FROM Job j WHERE j.customer.id = :customerId")
    List<Job> findJobsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.owner.id = :ownerId AND j.status NOT IN ('DONE','CANCELLED')")
    long countActiveJobsByOwner(@Param("ownerId") Long ownerId);

    // Customer delete ke liye — customer FK null karo directly in DB
    @Modifying
    @Query("UPDATE Job j SET j.customer = null WHERE j.customer.id = :customerId")
    void detachCustomerFromJobs(@Param("customerId") Long customerId);
}