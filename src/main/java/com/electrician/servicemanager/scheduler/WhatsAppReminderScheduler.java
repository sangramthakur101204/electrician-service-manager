package com.electrician.servicemanager.scheduler;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.repository.CustomerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class WhatsAppReminderScheduler {

    private final CustomerRepository customerRepository;

    public WhatsAppReminderScheduler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Runs every day at 9:00 AM.
     * Logs WhatsApp reminder URLs for:
     *   1. Customers with PENDING service
     *   2. Customers with warranty expiring in next 30 days
     *
     * In production, replace System.out.println with actual
     * WhatsApp Business API calls (e.g., Twilio, WATI, etc.)
     */
    // DISABLED: ReminderService already handles 9AM warranty check — this was duplicate
    // @Scheduled(cron = "0 0 9 * * ?")
    public void sendWhatsAppReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate next30 = today.plusDays(30);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        // ── Pending Service Reminders ──
        List<Customer> pendingServices = customerRepository.findAll().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getServiceStatus()))
                .toList();

        if (!pendingServices.isEmpty()) {
            System.out.println("\n========= PENDING SERVICE REMINDERS =========");
            pendingServices.forEach(c -> {
                String message = "⚠️ *Service Reminder*\n\n" +
                        "Hello *" + c.getName() + "*,\n\n" +
                        "Your *" + c.getMachineType() + "* (" + c.getMachineBrand() +
                        " - " + c.getModel() + ") service is still *PENDING*.\n\n" +
                        "Please schedule your service appointment. ⚡";
                String url = buildWhatsAppUrl(c.getMobile(), message);
                System.out.println("[PENDING] " + c.getName() + " (" + c.getMobile() + "): " + url);
            });
        }

        // ── Warranty Expiry Reminders ──
        List<Customer> expiringWarranty = customerRepository.findAll().stream()
                .filter(c -> c.getWarrantyEnd() != null &&
                        !c.getWarrantyEnd().isBefore(today) &&
                        !c.getWarrantyEnd().isAfter(next30))
                .toList();

        if (!expiringWarranty.isEmpty()) {
            System.out.println("\n========= WARRANTY EXPIRY REMINDERS =========");
            expiringWarranty.forEach(c -> {
                String message = "⏰ *Warranty Expiry Reminder*\n\n" +
                        "Hello *" + c.getName() + "*,\n\n" +
                        "Your *" + c.getMachineType() + "* (" + c.getMachineBrand() +
                        " - " + c.getModel() + ") warranty is expiring on *" +
                        c.getWarrantyEnd().format(fmt) + "*.\n\n" +
                        "Contact us to extend or check your machine. ⚡";
                String url = buildWhatsAppUrl(c.getMobile(), message);
                System.out.println("[WARRANTY] " + c.getName() + " (" + c.getMobile() + "): " + url);
            });
        }

        System.out.println("\n[Scheduler] Done. Pending: " + pendingServices.size() +
                ", Expiring warranty: " + expiringWarranty.size());
    }

    private String buildWhatsAppUrl(String mobile, String message) {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/91" + mobile + "?text=" + encoded;
    }
}