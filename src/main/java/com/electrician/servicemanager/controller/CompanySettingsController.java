package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.CompanySettings;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CompanySettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@CrossOrigin(origins = "*")
public class CompanySettingsController {

    private final CompanySettingsRepository settingsRepository;

    public CompanySettingsController(CompanySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping
    public ResponseEntity<CompanySettings> getSettings(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        return settingsRepository.findByOwnerId(owner.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new CompanySettings()));
    }

    @PostMapping
    public ResponseEntity<CompanySettings> saveSettings(@RequestBody CompanySettings settings,
                                                        HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        settings.setOwnerId(owner.getId());
        return ResponseEntity.ok(settingsRepository.save(settings));
    }

    @PutMapping
    public ResponseEntity<CompanySettings> updateSettings(@RequestBody CompanySettings updated,
                                                          HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        updated.setOwnerId(owner.getId());
        return ResponseEntity.ok(settingsRepository.save(updated));
    }
}