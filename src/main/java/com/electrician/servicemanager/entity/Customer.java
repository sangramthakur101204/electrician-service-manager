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
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Customer Info ──────────────────────────────
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 10)
    private String mobile;

    @Column(length = 500)
    private String address;          // Full address (from GPS or manual)

    private Double latitude;         // Auto-filled from GPS
    private Double longitude;        // Auto-filled from GPS

    // ── Machine Info ───────────────────────────────
    @Column(nullable = false)
    private String machineType;      // AC, Washing Machine, etc.

    @Column(nullable = false)
    private String machineBrand;     // LG, Samsung, etc.

    private String model;
    private String serialNumber;

    // ── Service Info ───────────────────────────────
    private LocalDate serviceDate;   // Service ki date (replaces purchaseDate)
    private String warrantyPeriod;   // "3 months" / "6 months" / "1 year" / "2 years"
    private LocalDate warrantyEnd;   // Auto-calculated from serviceDate + warrantyPeriod

    @Column(length = 1000)
    private String serviceDetails;   // Kya kaam kiya — parts replaced, issue fixed, etc.

    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String serviceStatus;    // PENDING / DONE

    @Column(length = 500)
    private String notes;            // Extra remarks

    // ── Owner link (multi-tenant support) ─────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password","owner"})
    private com.electrician.servicemanager.entity.User owner;
}