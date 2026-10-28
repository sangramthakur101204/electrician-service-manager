package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.Job;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CustomerRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Public, single-business customer booking entry point. Add OTP/CAPTCHA before production launch. */
@RestController
@RequestMapping("/customer-portal")
@CrossOrigin(origins = "*")
@Validated
public class CustomerPortalController {
    private final UserRepository users;
    private final CustomerRepository customers;
    private final JobRepository jobs;

    @Value("${electroserve.customer-portal.owner-id:}")
    private String configuredOwnerId;

    @Value("${electroserve.customer-portal.enabled:false}")
    private boolean portalEnabled;

    public CustomerPortalController(UserRepository users, CustomerRepository customers, JobRepository jobs) {
        this.users = users;
        this.customers = customers;
        this.jobs = jobs;
    }

    @GetMapping("/config")
    public ResponseEntity<?> config() {
        if (!portalEnabled) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Customer portal is disabled until public-booking protections are configured."));
        User owner = configuredOwner();
        if (owner == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Customer portal is not configured. Set CUSTOMER_PORTAL_OWNER_ID."));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessName", "Matoshree / Samarth Enterprises");
        result.put("bookingEnabled", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest request) {
        if (!portalEnabled) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Customer portal is disabled until public-booking protections are configured."));
        User owner = configuredOwner();
        if (owner == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Customer portal is not configured. Set CUSTOMER_PORTAL_OWNER_ID."));

        LocalDate scheduledDate;
        try {
            scheduledDate = request.getScheduledDate() == null || request.getScheduledDate().isBlank()
                    ? LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
                    : LocalDate.parse(request.getScheduledDate());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledDate must use YYYY-MM-DD format."));
        }
        if (scheduledDate.isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please choose today or a future date."));
        }

        String mobile = request.getMobile().replaceAll("\\D", "");
        if (mobile.length() != 10) return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid 10-digit Indian mobile number."));

        Customer customer = customers.findCustomersByOwner(owner.getId()).stream()
                .filter(c -> c.getOwner() != null && owner.getId().equals(c.getOwner().getId()))
                .filter(c -> mobile.equals(c.getMobile()))
                .findFirst().orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setName(request.getName().trim());
            customer.setMobile(mobile);
            customer.setOwner(owner);
            customer.setMachineType(request.getAppliance().trim());
            customer.setMachineBrand(request.getBrand() == null || request.getBrand().isBlank() ? "Not specified" : request.getBrand().trim());
            customer.setAddress(request.getAddress().trim());
            customer.setServiceStatus("PENDING");
            customer.setWarrantyPeriod("No Warranty");
            customer = customers.save(customer);
        } else {
            // Keep the customer record current without changing their saved appliance history.
            customer.setName(request.getName().trim());
            customer.setAddress(request.getAddress().trim());
            customer = customers.save(customer);
        }

        Job job = new Job();
        job.setOwner(owner);
        job.setCustomer(customer);
        job.setCustomerName(request.getName().trim());
        job.setCustomerMobile(mobile);
        job.setCustomerAddress(request.getAddress().trim());
        job.setMachineType(request.getAppliance().trim());
        job.setMachineBrand(request.getBrand() == null || request.getBrand().isBlank() ? "Not specified" : request.getBrand().trim());
        job.setProblemDescription(request.getIssue().trim());
        job.setNotes(request.getNotes() == null ? "Customer app booking request" : request.getNotes().trim());
        job.setPriority("NORMAL");
        job.setStatus("NEW");
        job.setScheduledDate(scheduledDate);
        job.setScheduledTime(request.getScheduledTime() == null || request.getScheduledTime().isBlank() ? "To be confirmed" : request.getScheduledTime().trim());
        job.setCreatedAt(LocalDateTime.now());
        job = jobs.save(job);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookingId", job.getId());
        response.put("status", job.getStatus());
        response.put("message", "Service request received. The team will confirm the appointment.");
        response.put("businessName", "Matoshree / Samarth Enterprises");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private User configuredOwner() {
        if (configuredOwnerId == null || configuredOwnerId.isBlank()) return null;
        try {
            User user = users.findById(Long.parseLong(configuredOwnerId.trim())).orElse(null);
            return user != null && "OWNER".equalsIgnoreCase(user.getRole()) ? user : null;
        } catch (NumberFormatException ex) { return null; }
    }

    public static class BookingRequest {
        @NotBlank @Size(max=100) private String name;
        @NotBlank @Pattern(regexp="[+0-9()\\-\\s]{10,18}") private String mobile;
        @NotBlank @Size(max=500) private String address;
        @NotBlank @Size(max=80) private String appliance;
        @Size(max=100) private String brand;
        @NotBlank @Size(max=500) private String issue;
        private String scheduledDate;
        @Size(max=80) private String scheduledTime;
        @Size(max=1000) private String notes;
        public String getName(){return name;} public void setName(String v){name=v;}
        public String getMobile(){return mobile;} public void setMobile(String v){mobile=v;}
        public String getAddress(){return address;} public void setAddress(String v){address=v;}
        public String getAppliance(){return appliance;} public void setAppliance(String v){appliance=v;}
        public String getBrand(){return brand;} public void setBrand(String v){brand=v;}
        public String getIssue(){return issue;} public void setIssue(String v){issue=v;}
        public String getScheduledDate(){return scheduledDate;} public void setScheduledDate(String v){scheduledDate=v;}
        public String getScheduledTime(){return scheduledTime;} public void setScheduledTime(String v){scheduledTime=v;}
        public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    }
}
