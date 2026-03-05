package com.electrician.servicemanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rate_cards")
public class RateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;        // AC, Washing Machine, TV, Fan, etc.

    @Column(nullable = false)
    private String serviceName;     // Gas Refilling, Motor Repair, etc.

    @Column(length = 500)
    private String description;     // What's included

    @Column(nullable = false)
    private Double price;           // Base price in ₹

    private String unit;            // "per visit", "per piece", etc.

    private Boolean isActive = true;
}