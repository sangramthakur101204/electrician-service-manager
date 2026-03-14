package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/technicians")
@CrossOrigin(origins = "*")
public class TechnicianController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;
    private final com.electrician.servicemanager.repository.TechSessionRepository techSessionRepository;
    private final com.electrician.servicemanager.repository.TechnicianLocationRepository locationRepository;

    public TechnicianController(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JobRepository jobRepository,
                                com.electrician.servicemanager.repository.TechSessionRepository techSessionRepository,
                                com.electrician.servicemanager.repository.TechnicianLocationRepository locationRepository) {
        this.userRepository        = userRepository;
        this.passwordEncoder       = passwordEncoder;
        this.jobRepository         = jobRepository;
        this.techSessionRepository = techSessionRepository;
        this.locationRepository    = locationRepository;
    }

    // ── Owner ke saare technicians ────────────────────────────────────────────
    // GET /technicians
    @GetMapping
    public ResponseEntity<List<User>> getMyTechnicians(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        List<User> technicians = userRepository.findTechsByOwnerAndRole(owner.getId(), "TECHNICIAN");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        technicians.forEach(t -> {
            boolean changed = false;
            // Auto-fix stale isActive: if active=true but no session started → force inactive
            if (Boolean.TRUE.equals(t.getIsActive()) && t.getActiveStartedAt() == null) {
                t.setIsActive(false);
                changed = true;
            }
            // Reset daily counter if it's a new day
            if (t.getLastActiveDate() != null && !today.equals(t.getLastActiveDate())
                    && !Boolean.TRUE.equals(t.getIsActive())) {
                t.setTodayActiveMins(0);
                t.setLastActiveDate(today);
                changed = true;
            }
            if (changed) userRepository.save(t);
        });
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
    // Optional query param: ?active=true or ?active=false
    // If not provided → blind flip (backward compat)
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActive(@PathVariable Long id,
                                          @RequestParam(required = false) Boolean active,
                                          HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");

        return userRepository.findById(id).map(tech -> {
            // If caller passes desired state explicitly → use it; else flip
            boolean goingActive = (active != null) ? active : !Boolean.TRUE.equals(tech.getIsActive());
            // Idempotent: already in desired state → just return current status
            if (active != null && Boolean.TRUE.equals(tech.getIsActive()) == active) {
                return ResponseEntity.ok(Map.of(
                        "message", tech.getName() + (active ? " already Active" : " already Inactive"),
                        "todayActiveMins", tech.getTodayActiveMins() != null ? tech.getTodayActiveMins() : 0
                ));
            }
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

            if (goingActive) {
                // Going Active — reset daily counter if new day, then save start time
                if (!today.equals(tech.getLastActiveDate())) {
                    tech.setTodayActiveMins(0);
                    tech.setLastActiveDate(today);
                }
                tech.setIsActive(true);
                tech.setActiveStartedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            } else {
                // Going Inactive — calculate this session's duration and add to today's total
                if (tech.getActiveStartedAt() != null) {
                    long sessionMins = ChronoUnit.MINUTES.between(
                            tech.getActiveStartedAt(),
                            LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                    // If date rolled over during session, reset first
                    if (!today.equals(tech.getLastActiveDate())) {
                        tech.setTodayActiveMins((int) sessionMins);
                        tech.setLastActiveDate(today);
                    } else {
                        int prev = tech.getTodayActiveMins() != null ? tech.getTodayActiveMins() : 0;
                        tech.setTodayActiveMins((int)(prev + sessionMins));
                    }
                }
                tech.setIsActive(false);
                tech.setActiveStartedAt(null);
            }
            userRepository.save(tech);
            String status = tech.getIsActive() ? "Active" : "Inactive";
            // Push real-time update to owner via SSE
            Long ownerId = tech.getOwnerId();
            if (ownerId != null) {
                TechStatusSSEController.pushStatusUpdate(ownerId, tech.getId(), tech.getName(), tech.getIsActive());
            }
            return ResponseEntity.ok(Map.of(
                    "message", tech.getName() + " " + status + " kar diya",
                    "isOnline", tech.getIsActive(),
                    "todayActiveMins", tech.getTodayActiveMins() != null ? tech.getTodayActiveMins() : 0
            ));
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
            // Pehle related data delete karo (foreign key constraint fix)
            List<com.electrician.servicemanager.entity.TechSession> sessions =
                    techSessionRepository.findByTechnicianId(tech.getId());
            techSessionRepository.deleteAll(sessions);
            locationRepository.deleteByTechnicianId(tech.getId());
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