package com.electrician.servicemanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_machines")
public class CustomerMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","machines","owner"})
    private Customer customer;

    @Column(nullable = false)
    private String machineType;       // AC, Washing Machine, etc.

    @Column(nullable = false)
    private String machineBrand;      // LG, Samsung, etc.

    private String model;
    private String serialNumber;
    private String notes;

    private LocalDate purchaseDate;   // Kab kharida (optional)

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
    }
}