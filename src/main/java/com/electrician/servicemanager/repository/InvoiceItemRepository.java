package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Query("SELECT i FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
    List<InvoiceItem> findItemsByInvoice(@Param("invoiceId") Long invoiceId);
}