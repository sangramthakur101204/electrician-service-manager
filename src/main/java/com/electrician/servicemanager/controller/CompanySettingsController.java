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

    private final com.electrician.servicemanager.repository.RateCardRepository rateCardRepository;

    public CompanySettingsController(CompanySettingsRepository settingsRepository,
                                     com.electrician.servicemanager.repository.RateCardRepository rateCardRepository) {
        this.settingsRepository = settingsRepository;
        this.rateCardRepository = rateCardRepository;
    }

    @GetMapping
    public ResponseEntity<CompanySettings> getSettings(HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if (user == null) return ResponseEntity.status(401).build();
        // Technician ke liye owner ki settings return karo
        Long lookupId = "TECHNICIAN".equalsIgnoreCase(user.getRole()) && user.getOwnerId() != null
                ? user.getOwnerId()
                : user.getId();
        return settingsRepository.findByOwnerId(lookupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new CompanySettings()));
    }

    @PostMapping
    public ResponseEntity<CompanySettings> saveSettings(@RequestBody CompanySettings settings,
                                                        HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        settings.setOwnerId(owner.getId());
        CompanySettings saved = settingsRepository.save(settings);
        if (settings.getRateCardJson() != null) syncRateCards(owner.getId(), settings.getRateCardJson());
        return ResponseEntity.ok(saved);
    }

    @PutMapping
    public ResponseEntity<CompanySettings> updateSettings(@RequestBody CompanySettings updated,
                                                          HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.status(401).build();
        updated.setOwnerId(owner.getId());
        CompanySettings saved = settingsRepository.save(updated);
        if (updated.getRateCardJson() != null) syncRateCards(owner.getId(), updated.getRateCardJson());
        return ResponseEntity.ok(saved);
    }
    // Sync rateCardJson to RateCard table for this owner
    private void syncRateCards(Long ownerId, String rateCardJson) {
        if (rateCardJson == null || rateCardJson.isBlank()) return;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, java.util.List<String>> map = om.readValue(rateCardJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, java.util.List<String>>>(){});

            // Delete existing owner rate cards
            java.util.List<com.electrician.servicemanager.entity.RateCard> existing =
                    rateCardRepository.findByOwnerIdAndIsActiveTrue(ownerId);
            rateCardRepository.deleteAll(existing);

            // Insert new ones
            java.util.List<com.electrician.servicemanager.entity.RateCard> newCards = new java.util.ArrayList<>();
            map.forEach((category, services) -> {
                if (services != null) services.forEach(svc -> {
                    // Parse "Service Name: ₹1500" format
                    String name = svc;
                    Double price = 0.0;
                    int idx = svc.lastIndexOf(":");
                    if (idx > 0) {
                        name = svc.substring(0, idx).trim();
                        String priceStr = svc.substring(idx+1).trim()
                                .replace("₹","").replace(",","").trim();
                        try { price = Double.parseDouble(priceStr); } catch(Exception e) {}
                    }
                    com.electrician.servicemanager.entity.RateCard rc = new com.electrician.servicemanager.entity.RateCard();
                    rc.setCategory(category);
                    rc.setServiceName(name);
                    rc.setPrice(price);
                    rc.setUnit("per visit");
                    rc.setIsActive(true);
                    rc.setOwnerId(ownerId);
                    newCards.add(rc);
                });
            });
            rateCardRepository.saveAll(newCards);
        } catch(Exception e) {
            System.err.println("[Settings] Rate card sync failed: " + e.getMessage());
        }
    }
}