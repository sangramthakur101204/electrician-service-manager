package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId")
    List<Invoice> findInvoicesByCustomer(@Param("customerId") Long customerId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByPaymentStatus(String status);
}