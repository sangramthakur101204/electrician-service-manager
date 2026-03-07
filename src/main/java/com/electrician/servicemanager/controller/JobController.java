package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.Job;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CustomerRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobRepository     jobRepository;
    private final UserRepository    userRepository;
    private final CustomerRepository customerRepository;

    public JobController(JobRepository jobRepo, UserRepository userRepo, CustomerRepository cusRepo) {
        this.jobRepository      = jobRepo;
        this.userRepository     = userRepo;
        this.customerRepository = cusRepo;
    }

    // ── GET ALL ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs(@RequestParam(required=false) String status, HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if ("TECHNICIAN".equals(user.getRole()))
            return ResponseEntity.ok(jobRepository.findByTechnicianId(user.getId()));
        List<Job> jobs = status != null
                ? jobRepository.findByOwnerIdAndStatus(user.getId(), status)
                : jobRepository.findByOwnerId(user.getId());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        return ResponseEntity.ok(jobRepository.findActivByTechnicianId(tech.getId()));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Job>> getMyHistory(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        return ResponseEntity.ok(jobRepository.findByTechnicianId(tech.getId()));
    }

    // ── CREATE JOB — auto customer create ────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest req2, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        Job job = new Job();
        job.setOwner(owner);
        job.setProblemDescription(req2.getProblemDescription());
        job.setMachineType(req2.getMachineType());
        job.setMachineBrand(req2.getMachineBrand());
        job.setPriority(req2.getPriority() != null ? req2.getPriority() : "NORMAL");
        job.setNotes(req2.getNotes());
        job.setScheduledDate(req2.getScheduledDate() != null ? LocalDate.parse(req2.getScheduledDate()) : LocalDate.now());
        job.setCreatedAt(LocalDateTime.now());

        // ── Customer: existing ya naya auto-create ──
        if (req2.getCustomerId() != null) {
            // Existing customer selected
            customerRepository.findById(req2.getCustomerId()).ifPresent(job::setCustomer);
        } else if (req2.getCustomerName() != null && req2.getCustomerMobile() != null) {
            // Auto-create new customer with basic info
            Customer c = new Customer();
            c.setName(req2.getCustomerName().trim());
            c.setMobile(req2.getCustomerMobile().trim());
            c.setAddress(req2.getCustomerAddress());
            c.setLatitude(req2.getLatitude());
            c.setLongitude(req2.getLongitude());
            c.setMachineType(req2.getMachineType());
            c.setMachineBrand(req2.getMachineBrand() != null ? req2.getMachineBrand() : "");
            c.setServiceStatus("PENDING");
            c.setWarrantyPeriod("1 year");
            c.setServiceDate(job.getScheduledDate());
            Customer saved = customerRepository.save(c);
            job.setCustomer(saved);
        }

        // ── Assign technician ──
        if (req2.getTechnicianId() != null) {
            userRepository.findById(req2.getTechnicianId()).ifPresent(tech -> {
                job.setTechnician(tech);
                job.setStatus("ASSIGNED");
            });
        } else {
            job.setStatus("NEW");
        }

        Job saved = jobRepository.save(job);
        String waMsg = saved.getTechnician() != null ? buildWhatsAppMsg(saved) : null;

        return ResponseEntity.ok(Map.of(
                "job",          saved,
                "whatsappMsg",  waMsg != null ? waMsg : "",
                "whatsappUrl",  waMsg != null
                        ? "https://wa.me/91" + saved.getTechnician().getMobile()
                          + "?text=" + java.net.URLEncoder.encode(waMsg, java.nio.charset.StandardCharsets.UTF_8)
                        : ""
        ));
    }

    // ── UPDATE JOB STATUS ─────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id,
                                       @RequestBody JobRequest req2,
                                       HttpServletRequest req) {
        return jobRepository.findById(id).map(job -> {
            if (req2.getStatus() != null) {
                job.setStatus(req2.getStatus());
                if ("DONE".equals(req2.getStatus())) job.setCompletedAt(LocalDateTime.now());
            }
            if (req2.getTechnicianId() != null) {
                userRepository.findById(req2.getTechnicianId()).ifPresent(tech -> {
                    job.setTechnician(tech);
                    job.setStatus("ASSIGNED");
                });
            }
            if (req2.getNotes() != null) job.setNotes(req2.getNotes());
            return ResponseEntity.ok(jobRepository.save(job));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── TECHNICIAN: JOB COMPLETE + CUSTOMER DATA FILL ────────────────────────
    /**
     * PUT /jobs/{id}/complete
     * Technician yeh call karta hai job complete karne ke baad.
     * Customer ka serial, serviceDate, warranty, kya kaam kiya — sab save hota hai.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeJob(@PathVariable Long id,
                                         @RequestBody CompleteRequest cr,
                                         HttpServletRequest req) {
        try {
            Job job = jobRepository.findById(id).orElse(null);
            if (job == null) return ResponseEntity.status(404).body(Map.of("error","Job not found: " + id));

            // 1. Mark job DONE
            job.setStatus("DONE");
            job.setCompletedAt(LocalDateTime.now());
            if (cr.getNotes() != null) job.setNotes(cr.getNotes());
            jobRepository.save(job);

            // 2. Update customer record if linked
            Customer customer = job.getCustomer();
            String customerMobile = null;
            String customerName   = null;
            String machineBrand   = job.getMachineBrand();
            String machineType    = job.getMachineType();

            if (customer != null) {
                customerMobile = customer.getMobile();
                customerName   = customer.getName();
                if (customer.getMachineBrand() != null) machineBrand = customer.getMachineBrand();
                if (customer.getMachineType()  != null) machineType  = customer.getMachineType();

                if (cr.getSerialNumber()   != null) customer.setSerialNumber(cr.getSerialNumber());
                if (cr.getServiceDetails() != null) customer.setServiceDetails(cr.getServiceDetails());
                if (cr.getWarrantyPeriod() != null) customer.setWarrantyPeriod(cr.getWarrantyPeriod());

                LocalDate sDate = cr.getServiceDate() != null
                        ? LocalDate.parse(cr.getServiceDate()) : LocalDate.now();
                customer.setServiceDate(sDate);

                String wp   = cr.getWarrantyPeriod() != null ? cr.getWarrantyPeriod() : "1 year";
                LocalDate wEnd = switch (wp) {
                    case "3 months" -> sDate.plusMonths(3);
                    case "6 months" -> sDate.plusMonths(6);
                    case "2 years"  -> sDate.plusYears(2);
                    case "3 years"  -> sDate.plusYears(3);
                    default         -> sDate.plusYears(1);  // "1 year" or anything else
                };
                customer.setWarrantyEnd(wEnd);
                customer.setServiceStatus("DONE");
                customerRepository.save(customer);
            } else {
                // job created without customer link — use inline fields
                customerMobile = job.getCustomerMobile();
                customerName   = job.getCustomerName();
            }

            // 3. Build WhatsApp thank-you URL for customer
            String waUrl = "";
            if (customerMobile != null && !customerMobile.isBlank()) {
                String techName = req.getAttribute("currentUser") != null
                        ? ((com.electrician.servicemanager.entity.User)req.getAttribute("currentUser")).getName()
                        : "Technician";
                String msg = "Namaste " + nvl(customerName) + " ji! 🙏\n\n"
                        + "Aapki " + nvl(machineType) + " (" + nvl(machineBrand) + ") ki service complete ho gayi.\n"
                        + (cr.getServiceDetails() != null ? "✅ Kaam kiya: " + cr.getServiceDetails() + "\n" : "")
                        + (cr.getWarrantyPeriod() != null ? "🛡️ Warranty: " + cr.getWarrantyPeriod() + "\n" : "")
                        + "\nKoi bhi problem ho toh call karein.\n"
                        + "- " + techName + ", Matoshree Enterprises";
                waUrl = "https://wa.me/91" + customerMobile
                        + "?text=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
            }

            return ResponseEntity.ok(Map.of(
                "message",      "Job complete ho gaya!",
                "jobId",        id,
                "whatsappUrl",  waUrl,
                "customerMobile", customerMobile != null ? customerMobile : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "trace", e.getClass().getSimpleName()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        if (!jobRepository.existsById(id)) return ResponseEntity.notFound().build();
        jobRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Job delete ho gaya"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildWhatsAppMsg(Job job) {
        String priority = "EMERGENCY".equals(job.getPriority()) ? "🚨 EMERGENCY JOB" : "🔧 NEW JOB ASSIGNED";
        return priority + "\n\nCustomer: " + job.getDisplayName()
                + "\nMobile: "  + nvl(job.getDisplayMobile())
                + "\nAddress: " + nvl(job.getDisplayAddress())
                + "\n\nProblem: " + job.getProblemDescription()
                + "\nMachine: "  + nvl(job.getMachineType())
                + (job.getMachineBrand() != null ? " - " + job.getMachineBrand() : "")
                + (job.getNotes() != null && !job.getNotes().isBlank() ? "\nNote: " + job.getNotes() : "")
                + "\n\n- Matoshree Enterprises";
    }
    private String nvl(String s) { return s != null ? s : "-"; }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public static class JobRequest {
        private Long   customerId;
        private String customerName, customerMobile, customerAddress;
        private Double latitude, longitude;
        private String problemDescription, machineType, machineBrand;
        private String priority, status, scheduledDate, notes;
        private Long   technicianId;

        public Long   getCustomerId()              { return customerId; }
        public void   setCustomerId(Long v)        { customerId = v; }
        public String getCustomerName()            { return customerName; }
        public void   setCustomerName(String v)    { customerName = v; }
        public String getCustomerMobile()          { return customerMobile; }
        public void   setCustomerMobile(String v)  { customerMobile = v; }
        public String getCustomerAddress()         { return customerAddress; }
        public void   setCustomerAddress(String v) { customerAddress = v; }
        public Double getLatitude()                { return latitude; }
        public void   setLatitude(Double v)        { latitude = v; }
        public Double getLongitude()               { return longitude; }
        public void   setLongitude(Double v)       { longitude = v; }
        public String getProblemDescription()      { return problemDescription; }
        public void   setProblemDescription(String v){ problemDescription = v; }
        public String getMachineType()             { return machineType; }
        public void   setMachineType(String v)     { machineType = v; }
        public String getMachineBrand()            { return machineBrand; }
        public void   setMachineBrand(String v)    { machineBrand = v; }
        public String getPriority()                { return priority; }
        public void   setPriority(String v)        { priority = v; }
        public Long   getTechnicianId()            { return technicianId; }
        public void   setTechnicianId(Long v)      { technicianId = v; }
        public String getStatus()                  { return status; }
        public void   setStatus(String v)          { status = v; }
        public String getScheduledDate()           { return scheduledDate; }
        public void   setScheduledDate(String v)   { scheduledDate = v; }
        public String getNotes()                   { return notes; }
        public void   setNotes(String v)           { notes = v; }
    }

    public static class CompleteRequest {
        private String serialNumber, serviceDate, warrantyPeriod, serviceDetails, notes;

        public String getSerialNumber()           { return serialNumber; }
        public void   setSerialNumber(String v)   { serialNumber = v; }
        public String getServiceDate()            { return serviceDate; }
        public void   setServiceDate(String v)    { serviceDate = v; }
        public String getWarrantyPeriod()         { return warrantyPeriod; }
        public void   setWarrantyPeriod(String v) { warrantyPeriod = v; }
        public String getServiceDetails()         { return serviceDetails; }
        public void   setServiceDetails(String v) { serviceDetails = v; }
        public String getNotes()                  { return notes; }
        public void   setNotes(String v)          { notes = v; }
    }
}
