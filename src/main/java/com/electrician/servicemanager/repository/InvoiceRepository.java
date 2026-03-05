package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomerId(Long customerId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByPaymentStatus(String status);
}