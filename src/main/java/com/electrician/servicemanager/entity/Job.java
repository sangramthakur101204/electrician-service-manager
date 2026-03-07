package com.electrician.servicemanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer details (existing ya naya)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    // Agar naya customer hai — naam + mobile store karo
    private String customerName;
    private String customerMobile;
    private String customerAddress;

    // Problem details
    private String problemDescription;
    private String machineType;
    private String machineBrand;

    // Priority: NORMAL ya EMERGENCY
    private String priority = "NORMAL";

    // Status: NEW → ASSIGNED → ON_THE_WAY → IN_PROGRESS → DONE → CANCELLED
    private String status = "NEW";

    // Assigned technician
    @ManyToOne
    @JoinColumn(name = "technician_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "owner", "password"})
    private User technician;

    // Owner jo ne assign kiya
    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "owner", "password"})
    private User owner;

    private LocalDate scheduledDate;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    private String notes;

    // Helper: customer ka naam kaunsa show kare
    public String getDisplayName() {
        if (customer != null) return customer.getName();
        return customerName != null ? customerName : "Unknown";
    }

    public String getDisplayMobile() {
        if (customer != null) return customer.getMobile();
        return customerMobile;
    }

    public String getDisplayAddress() {
        if (customer != null) return customer.getAddress();
        return customerAddress;
    }
}
