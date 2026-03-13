package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.CompanySettingsRepository;
import com.electrician.servicemanager.repository.CustomerMachineRepository;
import com.electrician.servicemanager.repository.CustomerRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.repository.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerMachineRepository machineRepository;
    private final CompanySettingsRepository settingsRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public CustomerController(CustomerRepository customerRepository,
                              JobRepository jobRepository,
                              InvoiceRepository invoiceRepository,
                              CustomerMachineRepository machineRepository,
                              CompanySettingsRepository settingsRepository) {
        this.customerRepository = customerRepository;
        this.jobRepository      = jobRepository;
        this.invoiceRepository  = invoiceRepository;
        this.machineRepository  = machineRepository;
        this.settingsRepository = settingsRepository;
    }

    // ── Company helpers ───────────────────────────────────

    private String compName(Long ownerId) {
        if (ownerId == null) return "Matoshree Enterprises";
        return settingsRepository.findByOwnerId(ownerId)
                .map(cs -> cs.getCompanyName() != null && !cs.getCompanyName().isBlank()
                        ? cs.getCompanyName() : "Matoshree Enterprises")
                .orElse("Matoshree Enterprises");
    }

    private String buildFooter(Long ownerId) {
        if (ownerId == null) return "";
        return settingsRepository.findByOwnerId(ownerId).map(cs -> {
            StringBuilder sb = new StringBuilder();
            if (cs.getCompanyPhone()   != null && !cs.getCompanyPhone().isBlank())
                sb.append("\n📞 ").append(cs.getCompanyPhone());
            if (cs.getCompanyPhone2()  != null && !cs.getCompanyPhone2().isBlank())
                sb.append("\n📞 ").append(cs.getCompanyPhone2());
            if (cs.getCompanyEmail()   != null && !cs.getCompanyEmail().isBlank())
                sb.append("\n✉️ ").append(cs.getCompanyEmail());
            if (cs.getCompanyAddress() != null && !cs.getCompanyAddress().isBlank()) {
                sb.append("\n📍 ").append(cs.getCompanyAddress());
                sb.append("\n🗺️ https://maps.google.com/?q=")
                        .append(URLEncoder.encode(cs.getCompanyAddress(), StandardCharsets.UTF_8));
            }
            if (cs.getLinksJson() != null && !cs.getLinksJson().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    var links = om.readValue(cs.getLinksJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,String>>>(){});
                    for (var l : links) {
                        String url = l.get("url"); String lbl = l.get("label");
                        if (url != null && !url.isBlank())
                            sb.append("\n🔗 ").append(lbl != null && !lbl.isBlank() ? lbl + ": " : "").append(url);
                    }
                } catch (Exception ignored) {}
            }
            return sb.length() > 0 ? "\n" + sb.toString().stripLeading() : "";
        }).orElse("");
    }

    // ── CRUD ──────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> addCustomer(@RequestBody Customer customer,
                                         HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner != null) customer.setOwner(owner);

        String mobile = customer.getMobile();
        if (mobile != null && !mobile.isBlank()) {
            List<Customer> existing = owner != null
                    ? customerRepository.findCustomersByOwner(owner.getId())
                    : customerRepository.findAll();
            boolean dup = existing.stream().anyMatch(c -> mobile.equals(c.getMobile()));
            if (dup) {
                Customer existingCust = existing.stream()
                        .filter(c -> mobile.equals(c.getMobile()))
                        .findFirst().orElse(null);
                return ResponseEntity.status(409).body(java.util.Map.of(
                        "error", "duplicate_mobile",
                        "message", "Yeh mobile number pehle se registered hai",
                        "existingId",   existingCust != null ? existingCust.getId()  : null,
                        "existingName", existingCust != null ? existingCust.getName() : ""
                ));
            }
        }

        if (customer.getServiceStatus() == null) customer.setServiceStatus("PENDING");
        customer.setWarrantyEnd(calcWarrantyEnd(customer.getServiceDate(), customer.getWarrantyPeriod()));
        return ResponseEntity.ok(customerRepository.save(customer));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        if (owner == null) return ResponseEntity.ok(customerRepository.findAll());
        return ResponseEntity.ok(customerRepository.findCustomersByOwner(owner.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return customerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer updated) {
        return customerRepository.findById(id).map(c -> {
            c.setName(updated.getName());
            c.setMobile(updated.getMobile());
            c.setAddress(updated.getAddress());
            c.setLatitude(updated.getLatitude());
            c.setLongitude(updated.getLongitude());
            c.setMachineType(updated.getMachineType());
            c.setMachineBrand(updated.getMachineBrand());
            c.setModel(updated.getModel());
            c.setSerialNumber(updated.getSerialNumber());
            c.setServiceDate(updated.getServiceDate());
            c.setWarrantyPeriod(updated.getWarrantyPeriod());
            c.setWarrantyEnd(calcWarrantyEnd(updated.getServiceDate(), updated.getWarrantyPeriod()));
            c.setServiceDetails(updated.getServiceDetails());
            c.setServiceStatus(updated.getServiceStatus());
            c.setNotes(updated.getNotes());
            return ResponseEntity.ok(customerRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        if (!customerRepository.existsById(id)) return ResponseEntity.notFound().build();
        machineRepository.deleteByCustomerId(id);
        jobRepository.detachCustomerFromJobs(id);
        invoiceRepository.findInvoicesByCustomer(id).forEach(invoiceRepository::delete);
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/complete/{id}")
    public ResponseEntity<Customer> markServiceDone(@PathVariable Long id) {
        return customerRepository.findById(id).map(c -> {
            c.setServiceStatus("DONE");
            return ResponseEntity.ok(customerRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── LOCATION ──────────────────────────────────────────

    @GetMapping("/map/{id}")
    public ResponseEntity<String> openMap(@PathVariable Long id) {
        return customerRepository.findById(id).map(c -> {
            String url;
            if (c.getLatitude() != null && c.getLongitude() != null)
                url = "https://www.google.com/maps?q=" + c.getLatitude() + "," + c.getLongitude();
            else if (c.getAddress() != null && !c.getAddress().isBlank())
                url = "https://www.google.com/maps/search/" + URLEncoder.encode(c.getAddress(), StandardCharsets.UTF_8);
            else
                url = "https://www.google.com/maps";
            return ResponseEntity.ok(url);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── WHATSAPP MESSAGES ─────────────────────────────────

    @GetMapping("/whatsapp/thankyou/{id}")
    public ResponseEntity<String> thankYouMessage(@PathVariable Long id, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        Long ownerId = owner != null ? owner.getId() : null;
        return customerRepository.findById(id).map(c -> {
            Long cOwner = c.getOwner() != null ? c.getOwner().getId() : ownerId;
            String co   = compName(cOwner);
            String msg  = "✅ *Service Complete — " + co + "*\n\n"
                    + "Hello *" + c.getName() + "*! 👋\n\n"
                    + "Thank you for trusting us. Your service has been completed successfully!\n\n"
                    + "🔧 *Service Summary:*\n"
                    + "• Machine: " + c.getMachineType() + " (" + c.getMachineBrand()
                    + (c.getModel() != null && !c.getModel().isBlank() ? " " + c.getModel() : "") + ")\n"
                    + "• Serial No: " + nvl(c.getSerialNumber()) + "\n"
                    + "• Service Date: " + fmt(c.getServiceDate()) + "\n"
                    + (c.getServiceDetails() != null && !c.getServiceDetails().isBlank()
                    ? "• Work Done: " + c.getServiceDetails() + "\n" : "")
                    + "\n🛡️ *Warranty:* " + nvl(c.getWarrantyPeriod())
                    + " (Valid till: *" + fmt(c.getWarrantyEnd()) + "*)\n\n"
                    + "Koi bhi problem ho toh humse zaroor contact karein. 🙏\n\n"
                    + "— *" + co + "*"
                    + buildFooter(cOwner);
            return ResponseEntity.ok(buildWaUrl(c.getMobile(), msg));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/whatsapp/warranty/{id}")
    public ResponseEntity<String> warrantyCardMessage(@PathVariable Long id, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        Long ownerId = owner != null ? owner.getId() : null;
        return customerRepository.findById(id).map(c -> {
            Long cOwner = c.getOwner() != null ? c.getOwner().getId() : ownerId;
            String co   = compName(cOwner);
            String certNo = "WC-" + String.format("%04d", c.getId()) + "-"
                    + LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
            String msg  = "🛡️ *WARRANTY CERTIFICATE*\n"
                    + "━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "*" + co + "* — Professional Service ⚡\n\n"
                    + "📋 *Certificate No:* " + certNo + "\n\n"
                    + "👤 *Customer:* " + c.getName() + "\n"
                    + "📞 *Mobile:* +91 " + c.getMobile() + "\n"
                    + "📍 *Address:* " + nvl(c.getAddress()) + "\n\n"
                    + "🔧 *Machine Details:*\n"
                    + "  • Type: "      + c.getMachineType()  + "\n"
                    + "  • Brand: "     + c.getMachineBrand() + "\n"
                    + "  • Model: "     + nvl(c.getModel())       + "\n"
                    + "  • Serial No: " + nvl(c.getSerialNumber()) + "\n\n"
                    + "🛠️ *Work Done:*\n  " + nvl(c.getServiceDetails()) + "\n\n"
                    + "📅 *Service Date:* "   + fmt(c.getServiceDate())    + "\n"
                    + "✅ *Warranty Period:* " + nvl(c.getWarrantyPeriod()) + "\n"
                    + "⏳ *Valid Till:* *"     + fmt(c.getWarrantyEnd())    + "*\n\n"
                    + "━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "_Ye aapka official warranty certificate hai. Please save karein._\n\n"
                    + "Dhanyawad! 🙏\n\n"
                    + "— *" + co + "*"
                    + buildFooter(cOwner);
            return ResponseEntity.ok(buildWaUrl(c.getMobile(), msg));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/whatsapp/reminder/{id}")
    public ResponseEntity<String> reminderMessage(@PathVariable Long id, HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        Long ownerId = owner != null ? owner.getId() : null;
        return customerRepository.findById(id).map(c -> {
            Long cOwner = c.getOwner() != null ? c.getOwner().getId() : ownerId;
            String co   = compName(cOwner);
            boolean isPending = "PENDING".equalsIgnoreCase(c.getServiceStatus());
            String msg;
            if (isPending) {
                msg = "⚠️ *Service Reminder — " + co + "*\n\n"
                        + "Hello *" + c.getName() + "*,\n\n"
                        + "Aapki *" + c.getMachineType() + "* ("
                        + c.getMachineBrand()
                        + (c.getModel() != null && !c.getModel().isBlank() ? " - " + c.getModel() : "") + ") "
                        + "ki service abhi *PENDING* hai.\n\n"
                        + "Please appointment schedule karein. 📞\n\n"
                        + "— *" + co + "*"
                        + buildFooter(cOwner);
            } else {
                msg = "⏰ *Warranty Expiry Reminder — " + co + "*\n\n"
                        + "Hello *" + c.getName() + "*,\n\n"
                        + "Aapki *" + c.getMachineType() + "* ("
                        + c.getMachineBrand()
                        + (c.getModel() != null && !c.getModel().isBlank() ? " - " + c.getModel() : "") + ") "
                        + "ki warranty *" + fmt(c.getWarrantyEnd()) + "* ko expire ho rahi hai.\n\n"
                        + "Service check ke liye humse contact karein. 📞\n\n"
                        + "— *" + co + "*"
                        + buildFooter(cOwner);
            }
            return ResponseEntity.ok(buildWaUrl(c.getMobile(), msg));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── SEARCH & FILTERS ──────────────────────────────────

    @GetMapping("/warranty-expiring")
    public ResponseEntity<List<Customer>> expiringSoon(HttpServletRequest req) {
        User owner = (User) req.getAttribute("currentUser");
        LocalDate today  = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate next30 = today.plusDays(30);
        List<Customer> all = owner != null
                ? customerRepository.findCustomersByOwner(owner.getId())
                : customerRepository.findAll();
        return ResponseEntity.ok(
                all.stream()
                        .filter(c -> c.getWarrantyEnd() != null
                                && !c.getWarrantyEnd().isBefore(today)
                                && !c.getWarrantyEnd().isAfter(next30))
                        .toList()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String machineType,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String serialNumber) {
        if (name         != null) return ResponseEntity.ok(customerRepository.findByNameContainingIgnoreCase(name));
        if (machineType  != null) return ResponseEntity.ok(customerRepository.findByMachineType(machineType));
        if (brand        != null) return ResponseEntity.ok(customerRepository.findByMachineBrand(brand));
        if (serialNumber != null) return ResponseEntity.ok(customerRepository.findBySerialNumber(serialNumber));
        return ResponseEntity.ok(customerRepository.findAll());
    }

    // ── HELPERS ───────────────────────────────────────────

    private LocalDate calcWarrantyEnd(LocalDate serviceDate, String period) {
        if (serviceDate == null || period == null) return null;
        return switch (period) {
            case "3 months" -> serviceDate.plusMonths(3);
            case "6 months" -> serviceDate.plusMonths(6);
            case "1 year"   -> serviceDate.plusYears(1);
            case "2 years"  -> serviceDate.plusYears(2);
            case "3 years"  -> serviceDate.plusYears(3);
            default         -> serviceDate.plusMonths(6);
        };
    }

    private String buildWaUrl(String mobile, String msg) {
        return "https://wa.me/91" + mobile + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
    }

    private String fmt(LocalDate d) { return d != null ? d.format(FMT) : "—"; }
    private String nvl(String s)    { return s != null && !s.isBlank() ? s : "—"; }
}