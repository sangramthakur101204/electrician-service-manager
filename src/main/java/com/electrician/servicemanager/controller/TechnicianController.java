package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/technicians")
@CrossOrigin(origins = "*")
public class TechnicianController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;

    public TechnicianController(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JobRepository jobRepository) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository   = jobRepository;
    }

    // ── Owner ke saare technicians ────────────────────────────────────────────
    // GET /technicians
    @GetMapping
    public ResponseEntity<List<User>> getMyTechnicians(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        List<User> technicians = userRepository.findTechsByOwnerAndRole(owner.getId(), "TECHNICIAN");
        return ResponseEntity.ok(technicians);
    }

    // ── Naya Technician Add Karo ──────────────────────────────────────────────
    // POST /technicians
    // Body: { name, mobile, password }
    @PostMapping
    public ResponseEntity<?> addTechnician(@RequestBody TechnicianRequest techReq,
                                           HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");

        if (userRepository.existsByMobile(techReq.getMobile())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Yeh mobile already registered hai"));
        }

        User tech = new User();
        tech.setName(techReq.getName());
        tech.setMobile(techReq.getMobile());
        tech.setPassword(passwordEncoder.encode(techReq.getPassword()));
        tech.setRole("TECHNICIAN");
        tech.setIsActive(true);
        tech.setOwner(owner);

        userRepository.save(tech);

        return ResponseEntity.ok(Map.of(
                "message", techReq.getName() + " ka account ban gaya!",
                "mobile", techReq.getMobile()
        ));
    }

    // ── Technician Edit Karo ──────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTechnician(@PathVariable Long id,
                                              @RequestBody TechnicianRequest techReq,
                                              HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");

        return userRepository.findById(id).map(tech -> {
            if (!tech.getOwnerId().equals(owner.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Tumhara technician nahi hai"));
            }
            tech.setName(techReq.getName());
            tech.setMobile(techReq.getMobile());
            if (techReq.getPassword() != null && !techReq.getPassword().isBlank()) {
                tech.setPassword(passwordEncoder.encode(techReq.getPassword()));
            }
            userRepository.save(tech);
            return ResponseEntity.ok(Map.of("message", "Updated!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Technician Active/Inactive ────────────────────────────────────────────
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActive(@PathVariable Long id, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");

        return userRepository.findById(id).map(tech -> {
            tech.setIsActive(!tech.getIsActive());
            userRepository.save(tech);
            String status = tech.getIsActive() ? "Active" : "Inactive";
            return ResponseEntity.ok(Map.of("message", tech.getName() + " " + status + " kar diya"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Technician Delete ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTechnician(@PathVariable Long id, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");

        return userRepository.findById(id).map(tech -> {
            if (!tech.getOwnerId().equals(owner.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Tumhara technician nahi hai"));
            }
            // Check for active/assigned jobs before deleting
            long activeJobs = jobRepository.findJobsByTechnician(tech.getId()).stream()
                    .filter(j -> !List.of("DONE","CANCELLED").contains(j.getStatus()))
                    .count();
            if (activeJobs > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", tech.getName() + " ke paas " + activeJobs + " active job(s) hain — pehle complete/cancel karo"
                ));
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", tech.getName() + " delete ho gaya"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DTO ───────────────────────────────────────────────────────────────────
    public static class TechnicianRequest {
        private String name;
        private String mobile;
        private String password;
        public String getName()           { return name; }
        public void setName(String v)     { name = v; }
        public String getMobile()         { return mobile; }
        public void setMobile(String v)   { mobile = v; }
        public String getPassword()       { return password; }
        public void setPassword(String v) { password = v; }
    }
}