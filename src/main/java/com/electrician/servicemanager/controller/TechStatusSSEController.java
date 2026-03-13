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

    // ownerId → list of active SSE emitters (owner can have multiple browser tabs)
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> ownerEmitters = new ConcurrentHashMap<>();

    public TechStatusSSEController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Owner subscribes to tech status updates ──────────────────────────────
    @GetMapping(value = "/tech-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new RuntimeException("Unauthorized"));
            return err;
        }

        Long ownerId = user.getId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // no timeout

        ownerEmitters.computeIfAbsent(ownerId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Send current state immediately on connect
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
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // Cleanup on disconnect
        emitter.onCompletion(() -> removeEmitter(ownerId, emitter));
        emitter.onTimeout(()    -> removeEmitter(ownerId, emitter));
        emitter.onError(e      -> removeEmitter(ownerId, emitter));

        return emitter;
    }

    private void removeEmitter(Long ownerId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = ownerEmitters.get(ownerId);
        if (list != null) list.remove(emitter);
    }

    // ── Called internally when tech goes online/offline ───────────────────────
    public static void pushStatusUpdate(Long ownerId, Long techId, String techName, boolean isOnline) {
        CopyOnWriteArrayList<SseEmitter> emitters = ownerEmitters.get(ownerId);
        if (emitters == null || emitters.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put("id",       techId);
        update.put("name",     techName);
        update.put("isOnline", isOnline);

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("update").data(update));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}