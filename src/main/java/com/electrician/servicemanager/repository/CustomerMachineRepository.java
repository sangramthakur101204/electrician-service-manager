package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.CustomerMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerMachineRepository extends JpaRepository<CustomerMachine, Long> {

    @Query("SELECT m FROM CustomerMachine m WHERE m.customer.id = :customerId")
    List<CustomerMachine> findByCustomerId(@Param("customerId") Long customerId);

    @Modifying
    @Query("DELETE FROM CustomerMachine m WHERE m.customer.id = :customerId")
    void deleteByCustomerId(@Param("customerId") Long customerId);
}