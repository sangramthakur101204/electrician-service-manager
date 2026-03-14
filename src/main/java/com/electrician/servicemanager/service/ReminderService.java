package com.electrician.servicemanager.service;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.repository.CustomerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled service — daily 9 AM check karta hai warranty expiry.
 * Frontend polling endpoint se owner dekh sakta hai.
 * WhatsApp links generate karta hai — owner ek click mein sab bhej sakta hai.
 */
@Service
public class ReminderService {

    private final CustomerRepository customerRepository;

    // In-memory pending reminders (reset daily)
    private List<Customer> pendingReminders = List.of();
    private LocalDate lastChecked = null;

    public ReminderService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Run every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkWarrantyExpiry() {
        runCheck();
    }

    public void runCheck() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate in30  = today.plusDays(30);
        lastChecked = today;
        pendingReminders = customerRepository.findAll().stream()
                .filter(c -> c.getWarrantyEnd() != null
                        && !c.getWarrantyEnd().isBefore(today)
                        && !c.getWarrantyEnd().isAfter(in30)
                        && c.getMobile() != null)
                .collect(Collectors.toList());
        System.out.println("[ReminderService] " + pendingReminders.size() + " warranty reminders pending for " + today);
    }

    public List<Customer> getPendingReminders() {
        // Sirf cached data return karo — DB hit NAHI karo har request pe
        // runCheck() sirf scheduled 9 AM pe chalega
        return pendingReminders;
    }

    public String buildWarrantyReminderMsg(Customer c) {
        int days = (int)(c.getWarrantyEnd().toEpochDay() - LocalDate.now(ZoneId.of("Asia/Kolkata")).toEpochDay());
        return "Namaste " + c.getName() + " ji! 🙏\n\n"
                + "Aapki *" + c.getMachineType() + " (" + c.getMachineBrand() + ")* ki warranty "
                + (days <= 0 ? "expire ho gayi hai ❌" : days + " din mein expire hogi ⚠️") + "\n"
                + "Warranty End Date: " + c.getWarrantyEnd() + "\n\n"
                + "AMC ya extended warranty ke liye contact karein.\n"
                + "- Matoshree Enterprises";
    }

    public String buildServiceReminderMsg(Customer c) {
        return "Namaste " + c.getName() + " ji! 🙏\n\n"
                + "Aapki *" + c.getMachineType() + " (" + c.getMachineBrand() + ")* ki service pending hai.\n"
                + "Jab bhi time mile, humse contact karein.\n\n"
                + "- Matoshree Enterprises";
    }
}