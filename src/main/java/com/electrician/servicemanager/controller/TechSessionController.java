package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.TechSession;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.TechSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tech-sessions")
@CrossOrigin(origins = "*")
public class TechSessionController {

    private final TechSessionRepository sessionRepo;

    public TechSessionController(TechSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    /** Technician goes Active → start a session */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        if (tech == null || !"TECHNICIAN".equals(tech.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Not a technician"));

        // Close any stale open session first
        sessionRepo.findOpenSession(tech.getId()).ifPresent(s -> {
            s.setEndTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            long mins = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
            s.setDurationMins((int) mins);
            sessionRepo.save(s);
        });

        TechSession session = new TechSession();
        session.setTechnicianId(tech.getId());
        session.setTechnicianName(tech.getName());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        session.setStartTime(now);
        session.setSessionDate(now.toLocalDate());
        sessionRepo.save(session);

        return ResponseEntity.ok(Map.of("message", "Session started", "sessionId", session.getId()));
    }

    /** Technician goes Inactive → end the session */
    @PostMapping("/end")
    public ResponseEntity<?> endSession(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        if (tech == null || !"TECHNICIAN".equals(tech.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Not a technician"));

        return sessionRepo.findOpenSession(tech.getId()).map(s -> {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            s.setEndTime(now);
            long mins = Duration.between(s.getStartTime(), now).toMinutes();
            s.setDurationMins((int) Math.max(mins, 0));
            sessionRepo.save(s);
            return ResponseEntity.ok(Map.of(
                    "message", "Session ended",
                    "durationMins", s.getDurationMins()
            ));
        }).orElse(ResponseEntity.ok(Map.of("message", "No open session")));
    }

    /**
     * Owner: GET /tech-sessions?techId=X&days=30
     * Returns per-day summary of active minutes for last N days.
     */
    @GetMapping
    public ResponseEntity<?> getSessions(
            @RequestParam(required = false) Long techId,
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req) {

        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate from  = today.minusDays(days - 1);

        List<TechSession> sessions = (techId != null)
                ? sessionRepo.findByTechIdAndDateRange(techId, from, today)
                : sessionRepo.findAllInDateRange(from, today);

        // Build per-day summary
        Map<String, Object> result = new LinkedHashMap<>();

        // Group by technicianId → date → sum minutes
        Map<Long, Map<LocalDate, Integer>> byTech = new LinkedHashMap<>();
        sessions.forEach(s -> {
            byTech.computeIfAbsent(s.getTechnicianId(), k -> new LinkedHashMap<>())
                    .merge(s.getSessionDate(),
                            s.getDurationMins() != null ? s.getDurationMins() : 0,
                            Integer::sum);
        });

        // Build day-by-day list for each tech
        List<Map<String, Object>> techSummaries = new ArrayList<>();
        byTech.forEach((tid, dateMap) -> {
            String name = sessions.stream()
                    .filter(s -> s.getTechnicianId().equals(tid))
                    .map(TechSession::getTechnicianName)
                    .findFirst().orElse("Tech " + tid);

            int todayMins = dateMap.getOrDefault(today, 0);

            List<Map<String, Object>> daily = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                LocalDate d = today.minusDays(i);
                int mins = dateMap.getOrDefault(d, 0);
                daily.add(Map.of(
                        "date",  d.toString(),
                        "label", d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")),
                        "mins",  mins,
                        "hrs",   String.format("%.1f", mins / 60.0)
                ));
            }

            techSummaries.add(Map.of(
                    "techId",    tid,
                    "name",      name,
                    "todayMins", todayMins,
                    "daily",     daily
            ));
        });

        // Also include today's raw sessions for timeline view
        List<Map<String, Object>> todaySessions = sessions.stream()
                .filter(s -> today.equals(s.getSessionDate()))
                .filter(s -> techId == null || s.getTechnicianId().equals(techId))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("techId",    s.getTechnicianId());
                    m.put("techName",  s.getTechnicianName());
                    m.put("start",     s.getStartTime().toString());
                    m.put("end",       s.getEndTime() != null ? s.getEndTime().toString() : null);
                    m.put("durationMins", s.getDurationMins() != null ? s.getDurationMins() : 0);
                    m.put("isActive",  s.getEndTime() == null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "summaries",     techSummaries,
                "todaySessions", todaySessions
        ));
    }

    /** Quick: GET /tech-sessions/today — today's total per tech (for AddTechnician page) */
    @GetMapping("/today")
    public ResponseEntity<?> getToday(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<TechSession> sessions = sessionRepo.findAllInDateRange(today, today);

        // Sum per tech
        Map<Long, Integer> minsByTech = new LinkedHashMap<>();
        Map<Long, String>  nameByTech = new LinkedHashMap<>();
        Map<Long, String>  lastActive = new LinkedHashMap<>();

        sessions.forEach(s -> {
            minsByTech.merge(s.getTechnicianId(),
                    s.getDurationMins() != null ? s.getDurationMins() : 0, Integer::sum);
            nameByTech.put(s.getTechnicianId(), s.getTechnicianName());
            // Most recent session end or "still active"
            if (!lastActive.containsKey(s.getTechnicianId())) {
                lastActive.put(s.getTechnicianId(),
                        s.getEndTime() != null ? s.getEndTime().toString() : "ACTIVE");
            }
        });

        List<Map<String, Object>> result = new ArrayList<>();
        minsByTech.forEach((tid, mins) -> result.add(Map.of(
                "techId",     tid,
                "name",       nameByTech.getOrDefault(tid, ""),
                "todayMins",  mins,
                "lastStatus", lastActive.getOrDefault(tid, "")
        )));

        return ResponseEntity.ok(result);
    }
}