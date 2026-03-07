package com.electrician.servicemanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDate invoiceDate;
    private LocalDate dueDate;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items;

    private Double subtotal    = 0.0;
    private Double discountAmt = 0.0;
    private Double taxPercent  = 0.0;
    private Double taxAmount   = 0.0;
    private Double totalAmount = 0.0;

    @Column(length = 500)
    private String notes;

    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'UNPAID'")
    private String paymentStatus;   // UNPAID / PAID

    private String paymentMethod;   // Cash / UPI / Pending

    private String technicianName;  // Serviced by: Raju
    private Long   jobId;           // Linked job
}
