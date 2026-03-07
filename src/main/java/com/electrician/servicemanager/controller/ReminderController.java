package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /** GET /reminders/warranty — expiring customers + WhatsApp URLs */
    @GetMapping("/warranty")
    public ResponseEntity<List<Map<String, Object>>> getWarrantyReminders() {
        List<Customer> list = reminderService.getPendingReminders();
        List<Map<String, Object>> result = list.stream().map(c -> {
            String msg = reminderService.buildWarrantyReminderMsg(c);
            String url = "https://wa.me/91" + c.getMobile()
                    + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            return Map.<String, Object>of(
                "id",           c.getId(),
                "name",         c.getName(),
                "mobile",       c.getMobile(),
                "machineType",  c.getMachineType() != null ? c.getMachineType() : "",
                "machineBrand", c.getMachineBrand() != null ? c.getMachineBrand() : "",
                "warrantyEnd",  c.getWarrantyEnd().toString(),
                "waUrl",        url,
                "message",      msg
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /reminders/service — pending service customers */
    @GetMapping("/service")
    public ResponseEntity<List<Map<String, Object>>> getServiceReminders() {
        // Pending service customers
        List<Map<String, Object>> result = reminderService.getPendingReminders().stream()
            .map(c -> {
                String msg = reminderService.buildServiceReminderMsg(c);
                String url = "https://wa.me/91" + c.getMobile()
                        + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
                return Map.<String, Object>of(
                    "id",      c.getId(),
                    "name",    c.getName(),
                    "mobile",  c.getMobile(),
                    "waUrl",   url
                );
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** POST /reminders/check — manual trigger karo */
    @PostMapping("/check")
    public ResponseEntity<?> triggerCheck() {
        reminderService.runCheck();
        return ResponseEntity.ok(Map.of(
            "message", "Check complete",
            "count",   reminderService.getPendingReminders().size()
        ));
    }
}
