package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sse")
@CrossOrigin(origins = "*")
public class TechStatusSSEController {

    private final UserRepository userRepository;

    // ownerId  → SSE emitters (owner watching tech list)
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> ownerEmitters = new ConcurrentHashMap<>();
    // techId   → SSE emitters (technician watching for new jobs)
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> techEmitters  = new ConcurrentHashMap<>();

    public TechStatusSSEController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Owner subscribes — watches tech online/offline ──────────────────────
    @GetMapping(value = "/tech-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeOwner(HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) { SseEmitter e = new SseEmitter(); e.completeWithError(new RuntimeException("Unauthorized")); return e; }

        Long ownerId = user.getId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ownerEmitters.computeIfAbsent(ownerId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Send current state on connect
        try {
            List<User> techs = userRepository.findTechsByOwnerAndRole(ownerId, "TECHNICIAN");
            List<Map<String, Object>> statusList = techs.stream().map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id",       t.getId());
                m.put("name",     t.getName());
                m.put("isOnline", Boolean.TRUE.equals(t.getIsActive()));
                return m;
            }).toList();
            emitter.send(SseEmitter.event().name("status").data(statusList));
        } catch (IOException e) { emitter.completeWithError(e); return emitter; }

        emitter.onCompletion(() -> removeOwner(ownerId, emitter));
        emitter.onTimeout(()    -> removeOwner(ownerId, emitter));
        emitter.onError(e      -> removeOwner(ownerId, emitter));
        return emitter;
    }

    // ── Technician subscribes — waits for new job notifications ─────────────
    @GetMapping(value = "/my-jobs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeTech(HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) { SseEmitter e = new SseEmitter(); e.completeWithError(new RuntimeException("Unauthorized")); return e; }

        Long techId = user.getId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        techEmitters.computeIfAbsent(techId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Send heartbeat so connection stays alive
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); }
        catch (IOException e) { emitter.completeWithError(e); return emitter; }

        emitter.onCompletion(() -> removeTech(techId, emitter));
        emitter.onTimeout(()    -> removeTech(techId, emitter));
        emitter.onError(e      -> removeTech(techId, emitter));
        return emitter;
    }

    // ── STATIC PUSH METHODS — called from other controllers ─────────────────

    // Push online/offline status to owner
    public static void pushStatusUpdate(Long ownerId, Long techId, String techName, boolean isOnline) {
        send(ownerEmitters.get(ownerId), "update", Map.of(
                "id", techId, "name", techName, "isOnline", isOnline));
    }

    // Push new job notification to technician
    public static void pushJobToTech(Long techId, Map<String, Object> jobData) {
        send(techEmitters.get(techId), "new-job", jobData);
    }

    // Generic send helper — removes dead emitters
    private static void send(CopyOnWriteArrayList<SseEmitter> emitters, String eventName, Object data) {
        if (emitters == null || emitters.isEmpty()) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter e : emitters) {
            try { e.send(SseEmitter.event().name(eventName).data(data)); }
            catch (IOException ex) { dead.add(e); }
        }
        emitters.removeAll(dead);
    }

    private void removeOwner(Long id, SseEmitter e) { CopyOnWriteArrayList<SseEmitter> l = ownerEmitters.get(id); if (l != null) l.remove(e); }
    private void removeTech(Long id,  SseEmitter e) { CopyOnWriteArrayList<SseEmitter> l = techEmitters.get(id);  if (l != null) l.remove(e); }
}