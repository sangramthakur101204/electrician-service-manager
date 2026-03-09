package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.CompanySettings;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CompanySettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@CrossOrigin(origins = "*")
public class CompanySettingsController {

    private final CompanySettingsRepository repo;

    public CompanySettingsController(CompanySettingsRepository repo) {
        this.repo = repo;
    }

    /** GET /settings — get current owner's company settings */
    @GetMapping
    public ResponseEntity<CompanySettings> getSettings(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        CompanySettings s = repo.findById(owner.getId())
            .orElseGet(() -> {
                // Return empty defaults
                CompanySettings def = new CompanySettings();
                def.setOwnerId(owner.getId());
                def.setCompanyName("Matoshree Enterprises");
                def.setCompanyAddress("");
                def.setCompanyPhone(owner.getMobile() != null ? owner.getMobile() : "");
                def.setGstNumber("");
                def.setTagline("Your trusted appliance repair service");
                return def;
            });
        return ResponseEntity.ok(s);
    }

    /** PUT /settings — save/update settings */
    @PutMapping
    public ResponseEntity<CompanySettings> saveSettings(
            @RequestBody CompanySettings body, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        body.setOwnerId(owner.getId());
        return ResponseEntity.ok(repo.save(body));
    }
}
