package com.electrician.servicemanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Persists technician live location to DB.
 * Survives backend restarts — no more "map goes blank after redeploy".
 * One row per technician (upsert by technicianId).
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "technician_locations")
public class TechnicianLocation {

    @Id
    private Long technicianId;   // same as User.id — one row per tech

    private String name;
    private String mobile;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;

    public TechnicianLocation(Long technicianId, String name, String mobile,
                               Double latitude, Double longitude) {
        this.technicianId = technicianId;
        this.name         = name;
        this.mobile       = mobile;
        this.latitude     = latitude;
        this.longitude    = longitude;
        this.updatedAt    = LocalDateTime.now();
    }
}
