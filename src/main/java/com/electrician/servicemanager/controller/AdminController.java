package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final String ADMIN_SECRET = "electroserve-admin-2025";

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Auth check
    private boolean isAdmin(String secret) {
        return ADMIN_SECRET.equals(secret);
    }

    // GET /admin/owners?secret=xxx — list all owners
    @GetMapping("/owners")
    public ResponseEntity<?> listOwners(@RequestParam String secret) {
        if (!isAdmin(secret)) return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        List<Map<String, Object>> owners = userRepository.findAll().stream()
                .filter(u -> "OWNER".equals(u.getRole()))
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id",        u.getId());
                    m.put("name",      u.getName() != null ? u.getName() : "");
                    m.put("mobile",    u.getMobile() != null ? u.getMobile() : "");
                    m.put("isBlocked", Boolean.TRUE.equals(u.getIsBlocked()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(owners);
    }

    // POST /admin/block?secret=xxx&userId=123 — block owner
    @PostMapping("/block")
    public ResponseEntity<?> blockOwner(@RequestParam String secret, @RequestParam Long userId) {
        if (!isAdmin(secret)) return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        return userRepository.findById(userId).map(u -> {
            u.setIsBlocked(true);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", u.getName() + " blocked successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // POST /admin/unblock?secret=xxx&userId=123 — unblock owner
    @PostMapping("/unblock")
    public ResponseEntity<?> unblockOwner(@RequestParam String secret, @RequestParam Long userId) {
        if (!isAdmin(secret)) return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        return userRepository.findById(userId).map(u -> {
            u.setIsBlocked(false);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", u.getName() + " unblocked successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}