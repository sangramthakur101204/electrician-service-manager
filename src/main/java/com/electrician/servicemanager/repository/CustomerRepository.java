package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCase(String name);
    List<Customer> findByMachineType(String machineType);
    List<Customer> findByMachineBrand(String machineBrand);
    List<Customer> findByModel(String model);
    List<Customer> findBySerialNumber(String serialNumber);
    List<Customer> findByServiceDate(LocalDate date);

    @Query("SELECT c FROM Customer c WHERE c.serviceDate BETWEEN :start AND :end")
    List<Customer> findByServiceDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Customer> findByWarrantyEnd(LocalDate date);

    @Query("SELECT c FROM Customer c WHERE c.warrantyEnd BETWEEN :start AND :end")
    List<Customer> findByWarrantyEndBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Customer> findByServiceStatus(String serviceStatus);

    @Query("SELECT c FROM Customer c WHERE c.owner.id = :ownerId OR c.owner IS NULL")
    List<Customer> findCustomersByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Customer c WHERE (c.owner.id = :ownerId OR c.owner IS NULL) AND c.serviceStatus = :status")
    List<Customer> findCustomersByOwnerAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);
}