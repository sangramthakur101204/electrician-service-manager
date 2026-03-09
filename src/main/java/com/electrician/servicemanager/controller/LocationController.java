package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.TechnicianLocation;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.TechnicianLocationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Live location — DB-persisted (survives backend restarts).
 * One row per technician, upserted on every POST.
 */
@RestController
@RequestMapping("/location")
@CrossOrigin(origins = "*")
public class LocationController {

    private final TechnicianLocationRepository locationRepo;

    public LocationController(TechnicianLocationRepository locationRepo) {
        this.locationRepo = locationRepo;
    }

    /** Technician: POST /location  { latitude, longitude } */
    @PostMapping
    public ResponseEntity<?> updateLocation(@RequestBody LocationRequest req,
                                            HttpServletRequest hreq) {
        User tech = (User) hreq.getAttribute("currentUser");
        if (tech == null) return ResponseEntity.status(401).build();

        if (!"TECHNICIAN".equals(tech.getRole())) {
            return ResponseEntity.ok(Map.of("message", "Owner location ignored"));
        }

        TechnicianLocation loc = locationRepo.findById(tech.getId())
            .orElse(new TechnicianLocation());
        loc.setTechnicianId(tech.getId());
        loc.setName(tech.getName());
        loc.setMobile(tech.getMobile());
        loc.setLatitude(req.getLatitude());
        loc.setLongitude(req.getLongitude());
        loc.setUpdatedAt(LocalDateTime.now());
        locationRepo.save(loc);

        return ResponseEntity.ok(Map.of("message", "Location updated"));
    }

    /** Owner: GET /location  → all technicians' latest location (within 2 hours) */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLocations(HttpServletRequest hreq) {
        User owner = (User) hreq.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();

        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);

        List<Map<String, Object>> live = locationRepo.findAll().stream()
            .filter(loc -> loc.getUpdatedAt() != null && loc.getUpdatedAt().isAfter(cutoff))
            .map(loc -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("techId",    loc.getTechnicianId());
                m.put("name",      loc.getName());
                m.put("mobile",    loc.getMobile());
                m.put("latitude",  loc.getLatitude());
                m.put("longitude", loc.getLongitude());
                m.put("updatedAt", loc.getUpdatedAt().toString());
                return m;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(live);
    }

    /** Technician: DELETE /location  → clear on logout */
    @DeleteMapping
    public ResponseEntity<?> clearLocation(HttpServletRequest hreq) {
        User tech = (User) hreq.getAttribute("currentUser");
        if (tech == null) return ResponseEntity.status(401).build();
        locationRepo.deleteById(tech.getId());
        return ResponseEntity.ok(Map.of("message", "Location cleared"));
    }

    public static class LocationRequest {
        private Double latitude, longitude;
        public Double getLatitude()          { return latitude;  }
        public void   setLatitude(Double v)  { latitude  = v; }
        public Double getLongitude()         { return longitude; }
        public void   setLongitude(Double v) { longitude = v; }
    }
}
