package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.UserRepository;
import com.electrician.servicemanager.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // ── ME — current user info (used by TechApp to sync active state) ─────────
    // GET /auth/me
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).build();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id",              user.getId());
        resp.put("name",            user.getName());
        resp.put("mobile",          user.getMobile());
        resp.put("role",            user.getRole());
        resp.put("isActive",        Boolean.TRUE.equals(user.getIsActive()));
        resp.put("activeStartedAt", user.getActiveStartedAt() != null
                ? user.getActiveStartedAt().toString() : null);
        return ResponseEntity.ok(resp);
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    // POST /auth/login
    // Body: { mobile, password }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByMobile(req.getMobile())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Mobile number registered nahi hai"));
        }


        if (user == null || user.getRole() == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Account nahi mila"));
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Password galat hai"));
        }

        // Check if owner account is blocked
        if ("OWNER".equals(user.getRole()) && Boolean.TRUE.equals(user.getIsBlocked())) {
            return ResponseEntity.status(403).body(Map.of("error", "Account suspended. Please contact support."));
        }
        // Check if technician's owner is blocked
        if ("TECHNICIAN".equals(user.getRole()) && user.getOwnerId() != null) {
            boolean ownerBlocked = userRepository.findById(user.getOwnerId())
                    .map(o -> Boolean.TRUE.equals(o.getIsBlocked())).orElse(false);
            if (ownerBlocked) {
                return ResponseEntity.status(403).body(Map.of("error", "Service suspended. Please contact your administrator."));
            }
        }

        // Reset isActive on login — fresh start (stale state fix)
        if ("TECHNICIAN".equals(user.getRole()) && Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(false);
            user.setActiveStartedAt(null);
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getMobile(), user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("mobile", user.getMobile());
        response.put("role", user.getRole());
        if (user.getOwnerId() != null) {
            response.put("ownerId", user.getOwnerId());
        }

        return ResponseEntity.ok(response);
    }

    // ── REGISTER OWNER ────────────────────────────────────────────────────────
    // POST /auth/register-owner

    @PostMapping("/register-owner")
    public ResponseEntity<?> registerOwner(@RequestBody RegisterRequest req) {
        if (userRepository.existsByMobile(req.getMobile())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Yeh mobile number already registered hai"));
        }

        User owner = new User();
        owner.setName(req.getName());
        owner.setMobile(req.getMobile());
        owner.setPassword(passwordEncoder.encode(req.getPassword()));
        owner.setRole("OWNER");
        owner.setIsActive(true);

        userRepository.save(owner);

        return ResponseEntity.ok(Map.of(
                "message", "Owner account ban gaya! Ab login karo.",
                "mobile", req.getMobile()
        ));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public static class LoginRequest {
        private String mobile;
        private String password;
        public String getMobile()            { return mobile; }
        public void setMobile(String v)      { mobile = v; }
        public String getPassword()          { return password; }
        public void setPassword(String v)    { password = v; }
    }

    public static class RegisterRequest {
        private String name;
        private String mobile;
        private String password;
        public String getName()              { return name; }
        public void setName(String v)        { name = v; }
        public String getMobile()            { return mobile; }
        public void setMobile(String v)      { mobile = v; }
        public String getPassword()          { return password; }
        public void setPassword(String v)    { password = v; }
    }
    // ── FCM Token Save ────────────────────────────────────────────────────────
    @PostMapping("/fcm-token")
    public ResponseEntity<?> saveFcmToken(@RequestBody Map<String, String> body, HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).build();
        String token = body.get("fcmToken");
        if (token != null && !token.isEmpty()) {
            user.setFcmToken(token);
            userRepository.save(user);
        }
        return ResponseEntity.ok(Map.of("message", "FCM token saved"));
    }

}