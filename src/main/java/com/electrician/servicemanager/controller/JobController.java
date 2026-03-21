package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.CompanySettings;
import com.electrician.servicemanager.entity.Job;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CompanySettingsRepository;
import com.electrician.servicemanager.repository.CustomerRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobRepository              jobRepository;
    private final UserRepository             userRepository;
    private final CustomerRepository         customerRepository;
    private final CompanySettingsRepository  settingsRepository;
    private final com.electrician.servicemanager.service.FCMService fcmService;
    private final com.electrician.servicemanager.repository.InvoiceRepository invoiceRepository;

    public JobController(JobRepository jobRepo, UserRepository userRepo,
                         CustomerRepository cusRepo, CompanySettingsRepository settingsRepo,
                         com.electrician.servicemanager.service.FCMService fcmService,
                         com.electrician.servicemanager.repository.InvoiceRepository invoiceRepo) {
        this.jobRepository      = jobRepo;
        this.userRepository     = userRepo;
        this.customerRepository = cusRepo;
        this.settingsRepository = settingsRepo;
        this.fcmService         = fcmService;
        this.invoiceRepository  = invoiceRepo;
    }

    // ── GET ALL ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs(@RequestParam(required=false) String status, HttpServletRequest req) {
        User user = (User) req.getAttribute("currentUser");
        if ("TECHNICIAN".equals(user.getRole()))
            return ResponseEntity.ok(jobRepository.findJobsByTechnician(user.getId()));
        List<Job> jobs = status != null
                ? jobRepository.findJobsByOwnerAndStatus(user.getId(), status)
                : jobRepository.findJobsByOwner(user.getId());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        return ResponseEntity.ok(jobRepository.findActiveJobsByTechnician(tech.getId()));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<java.util.LinkedHashMap>> getMyHistory(HttpServletRequest req) {
        User tech = (User) req.getAttribute("currentUser");
        List<Job> jobs = jobRepository.findJobsByTechnician(tech.getId());
        List<java.util.LinkedHashMap> result = new java.util.ArrayList<>();
        for (Job job : jobs) {
            java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",                 job.getId());
            m.put("status",             job.getStatus());
            m.put("problemDescription", job.getProblemDescription());
            m.put("machineType",        job.getMachineType());
            m.put("machineBrand",       job.getMachineBrand());
            m.put("priority",           job.getPriority());
            m.put("scheduledDate",      job.getScheduledDate() != null ? job.getScheduledDate().toString() : null);
            m.put("scheduledTime",      job.getScheduledTime());
            m.put("completedAt",        job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
            m.put("customer",           job.getCustomer());
            m.put("customerName",       job.getDisplayName());
            m.put("customerMobile",     job.getDisplayMobile());
            m.put("customerAddress",    job.getDisplayAddress());
            // Lookup invoiceId from invoice table
            java.util.Optional<com.electrician.servicemanager.entity.Invoice> inv = invoiceRepository.findByJobId(job.getId());
            if (inv.isPresent()) {
                m.put("invoiceId", inv.get().getId());
            }
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── CREATE JOB - auto customer create ────────────────────────────────────
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
        job.setScheduledDate(req2.getScheduledDate() != null ? LocalDate.parse(req2.getScheduledDate()) : LocalDate.now(ZoneId.of("Asia/Kolkata")));
        job.setScheduledTime(req2.getScheduledTime());
        job.setCreatedAt(LocalDateTime.now());
        // Save job location for route drawing in Live Tracking
        if (req2.getLatitude() != null)  job.setLatitude(req2.getLatitude());
        if (req2.getLongitude() != null) job.setLongitude(req2.getLongitude());

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
            if (req2.getCustomerDob() != null && !req2.getCustomerDob().isBlank()) {
                try { c.setDateOfBirth(java.time.LocalDate.parse(req2.getCustomerDob())); }
                catch(Exception ignored) {}
            }
            c.setLatitude(req2.getLatitude());
            c.setLongitude(req2.getLongitude());
            c.setMachineType(req2.getMachineType());
            c.setMachineBrand(req2.getMachineBrand() != null ? req2.getMachineBrand() : "");
            c.setServiceStatus("PENDING");
            c.setWarrantyPeriod("No Warranty");   // fixed: don't assume 1 year
            c.setServiceDate(job.getScheduledDate());
            c.setOwner(owner);    // Link customer to this owner
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

        // Push real-time notification to technician
        if (saved.getTechnician() != null) {
            pushJobNotification(saved);
        }

        // Tech WA URL
        String techWaUrl = waMsg != null && saved.getTechnician() != null
                ? "https://wa.me/91" + saved.getTechnician().getMobile()
                + "?text=" + java.net.URLEncoder.encode(waMsg, java.nio.charset.StandardCharsets.UTF_8)
                : "";
        // Customer WA URL
        String custMobileC = saved.getDisplayMobile();
        String custWaUrlC = "";
        if (custMobileC != null && !custMobileC.isBlank() && saved.getTechnician() != null) {
            CompanySettings _csC = saved.getOwner() != null
                    ? settingsRepository.findById(saved.getOwner().getId()).orElse(null) : null;
            String _compC = (_csC != null && _csC.getCompanyName() != null) ? _csC.getCompanyName() : "ElectroServe";
            String _footerC = saved.getOwner() != null ? buildFooter(saved.getOwner().getId()) : "";
            String techN = saved.getTechnician().getName();
            String techM = saved.getTechnician().getMobile();
            String dateC = saved.getScheduledDate() != null ? saved.getScheduledDate().toString() : "-";
            String timeC = saved.getScheduledTime() != null ? saved.getScheduledTime() : "";
            String whenC = timeC.isBlank() ? dateC : dateC + " " + timeC;
            String custMsg = "Namaste " + nvl(saved.getDisplayName()) + " ji!\n\n"
                    + "Aapka service request confirm ho gaya hai.\n\n"
                    + "Technician: " + techN + "\n"
                    + "Tech Mobile: " + techM + "\n"
                    + "Schedule: " + whenC + "\n"
                    + "Machine: " + nvl(saved.getMachineType()) + "\n\n"
                    + "Dhanyawad!\n\n- " + _compC + _footerC;
            custWaUrlC = "https://wa.me/91" + custMobileC + "?text="
                    + java.net.URLEncoder.encode(custMsg, java.nio.charset.StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok(Map.of(
                "job",           saved,
                "whatsappMsg",   waMsg != null ? waMsg : "",
                "whatsappUrl",   custWaUrlC,
                "techWhatsappUrl", techWaUrl
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
                if ("DONE".equals(req2.getStatus()))        job.setCompletedAt(LocalDateTime.now());
                if ("IN_PROGRESS".equals(req2.getStatus()) && job.getStartedAt() == null)
                    job.setStartedAt(LocalDateTime.now());
                if ("ON_THE_WAY".equals(req2.getStatus()) && job.getArrivedAt() == null)
                    job.setArrivedAt(LocalDateTime.now());

                // ── Sync customer serviceStatus ──────────────────────────────
                Customer cust = job.getCustomer();
                if (cust != null) {
                    if ("CANCELLED".equals(req2.getStatus())) {
                        cust.setServiceStatus("CANCELLED");
                        customerRepository.save(cust);
                    } else if ("DONE".equals(req2.getStatus())) {
                        cust.setServiceStatus("DONE");
                        customerRepository.save(cust);
                    }
                }
            }
            // Explicit timestamp overrides from TechApp
            if (req2.getArrivedAt() != null)  job.setArrivedAt(LocalDateTime.parse(req2.getArrivedAt()));
            if (req2.getStartedAt() != null)  job.setStartedAt(LocalDateTime.parse(req2.getStartedAt()));
            if (req2.getTechnicianId() != null) {
                userRepository.findById(req2.getTechnicianId()).ifPresent(tech -> {
                    job.setTechnician(tech);
                    job.setStatus("ASSIGNED");
                });
            }
            if (req2.getScheduledTime() != null) job.setScheduledTime(req2.getScheduledTime());
            if (req2.getNotes() != null) job.setNotes(req2.getNotes());
            if (req2.getLatitude()  != null) job.setLatitude(req2.getLatitude());
            if (req2.getLongitude() != null) job.setLongitude(req2.getLongitude());
            Job saved = jobRepository.save(job);

            // If technician just assigned → build auto-WA URL for customer
            if (req2.getTechnicianId() != null && saved.getTechnician() != null) {
                String custMobile = saved.getDisplayMobile();
                String techName   = saved.getTechnician().getName();
                String techMobile = saved.getTechnician().getMobile();
                String date  = saved.getScheduledDate() != null ? saved.getScheduledDate().toString() : "-";
                String time  = saved.getScheduledTime() != null ? saved.getScheduledTime() : "";
                String when  = time.isBlank() ? date : date + " " + time;
                CompanySettings _cs = saved.getOwner() != null
                        ? settingsRepository.findById(saved.getOwner().getId()).orElse(null) : null;
                String _compName = (_cs != null && _cs.getCompanyName() != null)
                        ? _cs.getCompanyName() : "Matoshree Enterprises";
                String _footer = saved.getOwner() != null ? buildFooter(saved.getOwner().getId()) : "";
                String waMsg = "🙏 Namaste " + nvl(saved.getDisplayName()) + " ji!\n\n"
                        + "Aapka service request confirm ho gaya hai. ✅\n\n"
                        + "👷 Technician: *" + techName + "*\n"
                        + "📞 Tech Mobile: " + techMobile + "\n"
                        + "📅 Schedule: *" + when + "*\n"
                        + "🔧 Machine: " + nvl(saved.getMachineType())
                        + (saved.getMachineBrand()!=null ? " - "+saved.getMachineBrand() : "") + "\n\n"
                        + "Koi problem ho toh humse directly contact karein.\n"
                        + "Dhanyawad! 🙏\n\n"
                        + "- " + _compName + _footer;
                String waUrl = custMobile != null && !custMobile.isBlank()
                        ? "https://wa.me/91" + custMobile + "?text=" + java.net.URLEncoder.encode(waMsg, java.nio.charset.StandardCharsets.UTF_8)
                        : null;
                // Push real-time notification to newly assigned technician
                pushJobNotification(saved);
                // Tech WA URL for reassign
                String techWaUrlR = "";
                if (saved.getTechnician() != null) {
                    String techWaMsgR = buildWhatsAppMsg(saved);
                    techWaUrlR = "https://wa.me/91" + saved.getTechnician().getMobile()
                            + "?text=" + java.net.URLEncoder.encode(techWaMsgR, java.nio.charset.StandardCharsets.UTF_8);
                }
                return ResponseEntity.ok(Map.of(
                        "job", saved,
                        "whatsappUrl", waUrl != null ? waUrl : "",
                        "techWhatsappUrl", techWaUrlR
                ));
            }
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── TECHNICIAN: JOB COMPLETE + CUSTOMER DATA FILL ────────────────────────
    /**
     * PUT /jobs/{id}/complete
     * Technician yeh call karta hai job complete karne ke baad.
     * Customer ka serial, serviceDate, warranty, kya kaam kiya - sab save hota hai.
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
                        ? LocalDate.parse(cr.getServiceDate()) : LocalDate.now(ZoneId.of("Asia/Kolkata"));
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
                // job created without customer link - use inline fields
                customerMobile = job.getCustomerMobile();
                customerName   = job.getCustomerName();
            }

            // 3. Build WhatsApp thank-you URL for customer
            String waUrl = "";
            if (customerMobile != null && !customerMobile.isBlank()) {
                String techName = req.getAttribute("currentUser") != null
                        ? ((com.electrician.servicemanager.entity.User)req.getAttribute("currentUser")).getName()
                        : "Technician";
                User techUser = (User) req.getAttribute("currentUser");
                String _cmpName2 = "Matoshree Enterprises";
                String _footer2 = "";
                if (job.getOwner() != null) {
                    CompanySettings _cs2 = settingsRepository.findById(job.getOwner().getId()).orElse(null);
                    if (_cs2 != null && _cs2.getCompanyName() != null) _cmpName2 = _cs2.getCompanyName();
                    _footer2 = buildFooter(job.getOwner().getId());
                }
                String msg = "Namaste " + nvl(customerName) + " ji! 🙏\n\n"
                        + "Aapki " + nvl(machineType) + " (" + nvl(machineBrand) + ") ki service complete ho gayi. ✅\n"
                        + (cr.getServiceDetails() != null ? "\n✅ Kaam kiya: " + cr.getServiceDetails() + "\n" : "")
                        + (cr.getWarrantyPeriod() != null && !"No Warranty".equals(cr.getWarrantyPeriod()) ? "🛡️ Warranty: " + cr.getWarrantyPeriod() + "\n" : "")
                        + "\nKoi bhi problem ho toh call karein.\n"
                        + "Dhanyawad! 🙏\n\n"
                        + "- " + techName + ", " + _cmpName2 + _footer2;
                waUrl = "https://wa.me/91" + customerMobile
                        + "?text=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
            }

            return ResponseEntity.ok(Map.of(
                    "message",        "Job complete ho gaya!",
                    "jobId",          id,
                    "whatsappUrl",    waUrl,
                    "customerMobile", customerMobile != null ? customerMobile : "",
                    "customer",       customer != null ? customer : Map.of()
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
    private void pushJobNotification(Job job) {
        if (job.getTechnician() == null) return;
        Long techId = job.getTechnician().getId();
        String priority = "EMERGENCY".equals(job.getPriority()) ? "EMERGENCY" : "NORMAL";
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("jobId",        job.getId());
        data.put("customerName", job.getDisplayName());
        data.put("address",      job.getDisplayAddress() != null ? job.getDisplayAddress() : "");
        data.put("machineType",  job.getMachineType() != null ? job.getMachineType() : "");
        data.put("priority",     priority);
        data.put("scheduledDate", job.getScheduledDate() != null ? job.getScheduledDate().toString() : "");
        data.put("scheduledTime", job.getScheduledTime() != null ? job.getScheduledTime() : "");
        TechStatusSSEController.pushJobToTech(techId, data);

        // FCM push notification - app band ho toh bhi aayega
        User tech = job.getTechnician();
        if (tech.getFcmToken() != null && !tech.getFcmToken().isEmpty()) {
            String title = "EMERGENCY".equals(job.getPriority())
                    ? "EMERGENCY Job!" : "Naya Job Assign Hua!";
            String body = job.getDisplayName() + " - " +
                    (job.getMachineType() != null ? job.getMachineType() : "") +
                    (job.getDisplayAddress() != null ? " | " + job.getDisplayAddress() : "");
            java.util.Map<String, String> fcmData = new java.util.HashMap<>();
            fcmData.put("jobId", String.valueOf(job.getId()));
            fcmData.put("priority", priority);
            fcmService.sendNotification(tech.getFcmToken(), title, body, fcmData);
        }
    }

    private String buildFooter(Long ownerId) {
        try {
            CompanySettings cs = settingsRepository.findById(ownerId).orElse(null);
            if (cs == null) return "";
            StringBuilder sb = new StringBuilder();
            if (cs.getCompanyPhone()  != null && !cs.getCompanyPhone().isBlank())  sb.append("\n📞 ").append(cs.getCompanyPhone());
            if (cs.getCompanyPhone2() != null && !cs.getCompanyPhone2().isBlank()) sb.append("\n📞 ").append(cs.getCompanyPhone2());
            if (cs.getCompanyEmail()  != null && !cs.getCompanyEmail().isBlank())  sb.append("\n✉️ ").append(cs.getCompanyEmail());
            if (cs.getCompanyAddress()!= null && !cs.getCompanyAddress().isBlank()) {
                sb.append("\n📍 ").append(cs.getCompanyAddress());
                sb.append("\n🗺️ https://maps.google.com/?q=")
                        .append(java.net.URLEncoder.encode(cs.getCompanyAddress(), java.nio.charset.StandardCharsets.UTF_8));
            }
            try {
                String linksJson = cs.getLinksJson();
                if (linksJson != null && !linksJson.isBlank()) {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode arr = om.readTree(linksJson);
                    for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                        String url = n.path("url").asText("");
                        String label = n.path("label").asText("");
                        if (!url.isBlank()) sb.append("\n🔗 ").append(label.isBlank() ? "" : label + ": ").append(url);
                    }
                }
            } catch(Exception ignore) {}
            return sb.length() > 0 ? "\n" + sb.toString() : "";
        } catch (Exception e) { return ""; }
    }

    private String buildWhatsAppMsg(Job job) {
        String compName = "Matoshree Enterprises";
        String footer = "";
        if (job.getOwner() != null) {
            CompanySettings cs = settingsRepository.findById(job.getOwner().getId()).orElse(null);
            if (cs != null && cs.getCompanyName() != null) compName = cs.getCompanyName();
            footer = buildFooter(job.getOwner().getId());
        }
        String priority = "EMERGENCY".equals(job.getPriority()) ? "🚨 EMERGENCY JOB" : "🔧 NEW JOB ASSIGNED";
        return priority + "\n\nCustomer: " + job.getDisplayName()
                + "\nMobile: "  + nvl(job.getDisplayMobile())
                + "\nAddress: " + nvl(job.getDisplayAddress())
                + "\n\nProblem: " + job.getProblemDescription()
                + "\nMachine: "  + nvl(job.getMachineType())
                + (job.getMachineBrand() != null ? " - " + job.getMachineBrand() : "")
                + (job.getNotes() != null && !job.getNotes().isBlank() ? "\nNote: " + job.getNotes() : "")
                + "\n\n- " + compName + footer;
    }
    private String nvl(String s) { return s != null ? s : "-"; }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public static class JobRequest {
        private Long   customerId;
        private String customerName, customerMobile, customerAddress;
        private String customerDob;
        private Double latitude, longitude;
        private String problemDescription, machineType, machineBrand;
        private String priority, status, scheduledDate, scheduledTime, notes;
        private Long   technicianId;
        private String arrivedAt, startedAt;   // ISO datetime strings from TechApp

        public Long   getCustomerId()              { return customerId; }
        public void   setCustomerId(Long v)        { customerId = v; }
        public String getCustomerDob()             { return customerDob; }
        public void   setCustomerDob(String v)     { customerDob = v; }
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
        public String getScheduledTime()           { return scheduledTime; }
        public void   setScheduledTime(String v)   { scheduledTime = v; }
        public String getNotes()                   { return notes; }
        public void   setNotes(String v)           { notes = v; }
        public String getArrivedAt()               { return arrivedAt; }
        public void   setArrivedAt(String v)       { arrivedAt = v; }
        public String getStartedAt()               { return startedAt; }
        public void   setStartedAt(String v)       { startedAt = v; }
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