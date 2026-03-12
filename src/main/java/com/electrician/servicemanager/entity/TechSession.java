package com.electrician.servicemanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row = one Active session of a technician.
 * Created when tech goes Active, endTime filled when they go Inactive.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "tech_sessions")
public class TechSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long technicianId;

    private String technicianName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;       // null = still active

    private Integer durationMins;        // filled on end

    private LocalDate sessionDate;       // date of startTime (for daily queries)
}