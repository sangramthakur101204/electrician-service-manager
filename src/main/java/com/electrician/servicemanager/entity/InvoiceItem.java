package com.electrician.servicemanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore                          // ← SIRF YEH LINE ADD KI HAI
    private Invoice invoice;

    private String serviceName;
    private String description;

    private Integer quantity = 1;
    private Double unitPrice = 0.0;
    private Double totalPrice = 0.0;
}