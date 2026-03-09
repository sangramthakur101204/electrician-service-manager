package com.electrician.servicemanager.scheduler;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.repository.CustomerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class ReminderScheduler {

    private final CustomerRepository customerRepository;

    public ReminderScheduler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Ye method har din 8 baje chalega
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate next30 = today.plusDays(30);

        // Pending service
        List<Customer> pendingServices = customerRepository.findAll().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getServiceStatus()))
                .toList();

        // Warranty expiring in next 30 days
        List<Customer> expiringWarranty = customerRepository.findAll().stream()
                .filter(c -> c.getWarrantyEnd() != null &&
                        !c.getWarrantyEnd().isBefore(today) &&
                        !c.getWarrantyEnd().isAfter(next30))
                .toList();

        // Console me reminders print karenge
        pendingServices.forEach(c -> System.out.println("Reminder: Service pending for " + c.getName()));
        expiringWarranty.forEach(c -> System.out.println("Reminder: Warranty expiring for " + c.getName()));
    }
}