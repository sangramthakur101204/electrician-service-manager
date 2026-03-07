package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory live location store.
 * Technician sends location every 30s → owner sees it on map.
 */
@RestController
@RequestMapping("/location")
@CrossOrigin(origins = "*")
public class LocationController {

    // technicianId → { lat, lng, name, mobile, updatedAt }
    private static final Map<Long, Map<String,Object>> locationStore = new ConcurrentHashMap<>();

    /** Technician: POST /location  { latitude, longitude } */
    @PostMapping
    public ResponseEntity<?> updateLocation(@RequestBody LocationRequest req, HttpServletRequest hreq) {
        User tech = (User) hreq.getAttribute("currentUser");
        if (tech == null) return ResponseEntity.status(401).build();

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("techId",    tech.getId());
        data.put("name",      tech.getName());
        data.put("mobile",    tech.getMobile());
        data.put("latitude",  req.getLatitude());
        data.put("longitude", req.getLongitude());
        data.put("updatedAt", LocalDateTime.now().toString());

        locationStore.put(tech.getId(), data);
        return ResponseEntity.ok(Map.of("message", "Location updated"));
    }

    /** Owner: GET /location  → all technicians' latest location */
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> getAllLocations(HttpServletRequest hreq) {
        User owner = (User) hreq.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        // Filter out stale (>10 min) locations
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Map<String,Object>> live = locationStore.values().stream()
            .filter(d -> {
                try {
                    return LocalDateTime.parse(d.get("updatedAt").toString()).isAfter(cutoff);
                } catch (Exception e) { return false; }
            })
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(live);
    }

    public static class LocationRequest {
        private Double latitude, longitude;
        public Double getLatitude()  { return latitude;  }
        public void setLatitude(Double v)  { latitude  = v; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double v) { longitude = v; }
    }
}
