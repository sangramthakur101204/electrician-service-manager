package com.electrician.servicemanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String mobile;

    @JsonIgnore
    private String password;

    // OWNER ya TECHNICIAN
    private String role;

    private Boolean isActive = true;

    // Jab tech Active karta hai tab ka timestamp — owner page pe duration calculate karne ke liye
    private LocalDateTime activeStartedAt;

    // Technician kis owner ka hai
    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    // Owner ka naam technician ke saath return karo
    public String getOwnerName() {
        return owner != null ? owner.getName() : null;
    }

    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}