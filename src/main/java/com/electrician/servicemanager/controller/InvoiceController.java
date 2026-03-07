package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.Invoice;
import com.electrician.servicemanager.entity.InvoiceItem;
import com.electrician.servicemanager.repository.CustomerRepository;
import com.electrician.servicemanager.repository.InvoiceItemRepository;
import com.electrician.servicemanager.repository.InvoiceRepository;
import com.electrician.servicemanager.service.InvoicePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final InvoiceRepository     invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final CustomerRepository    customerRepository;
    private final InvoicePdfService     pdfService;

    public InvoiceController(InvoiceRepository invoiceRepository,
                             InvoiceItemRepository itemRepository,
                             CustomerRepository customerRepository,
                             InvoicePdfService pdfService) {
        this.invoiceRepository  = invoiceRepository;
        this.itemRepository     = itemRepository;
        this.customerRepository = customerRepository;
        this.pdfService         = pdfService;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceRequest req) {
        try {
            if (req.getCustomerId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "customerId missing"));

            Customer customer = customerRepository.findById(req.getCustomerId()).orElse(null);
            if (customer == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Customer not found: " + req.getCustomerId()));

            // ── STEP 1: Calculate totals from items FIRST ────────────────
            double subtotal = 0;
            List<ItemRequest> validItems = new ArrayList<>();
            if (req.getItems() != null) {
                for (ItemRequest ir : req.getItems()) {
                    if (ir.getServiceName() == null || ir.getServiceName().isBlank()) continue;
                    validItems.add(ir);
                    int    qty   = ir.getQuantity() != null ? ir.getQuantity() : 1;
                    double price = nvl(ir.getUnitPrice());
                    subtotal += qty * price;
                }
            }
            double discount = nvl(req.getDiscountAmt());
            double taxPct   = nvl(req.getTaxPercent());
            double taxAmt   = (subtotal - discount) * taxPct / 100.0;
            double total    = subtotal - discount + taxAmt;

            // ── STEP 2: Build invoice with CORRECT totals ─────────────────
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setCustomer(customer);
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setDueDate(LocalDate.now().plusDays(7));
            invoice.setPaymentMethod(req.getPaymentMethod());
            invoice.setNotes(req.getNotes());
            invoice.setDiscountAmt(discount);
            invoice.setTaxPercent(taxPct);
            invoice.setTaxAmount(taxAmt);
            invoice.setSubtotal(subtotal);
            invoice.setTotalAmount(total);          // ← set BEFORE first save
            invoice.setTechnicianName(req.getTechnicianName());
            invoice.setJobId(req.getJobId());

            String method = req.getPaymentMethod();
            invoice.setPaymentStatus(
                    method != null && (method.equalsIgnoreCase("Cash") || method.equalsIgnoreCase("UPI"))
                            ? "PAID" : "UNPAID"
            );

            // ── STEP 3: Save invoice header (one save only) ───────────────
            Invoice saved = invoiceRepository.save(invoice);

            // ── STEP 4: Save items linked to invoice ─────────────────────
            List<InvoiceItem> savedItems = new ArrayList<>();
            for (ItemRequest ir : validItems) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(saved);
                item.setServiceName(ir.getServiceName());
                item.setDescription(ir.getDescription());
                item.setQuantity(ir.getQuantity() != null ? ir.getQuantity() : 1);
                item.setUnitPrice(nvl(ir.getUnitPrice()));
                item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
                savedItems.add(item);
            }
            itemRepository.saveAll(savedItems);
            saved.setItems(savedItems);             // attach for JSON response only

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Invoice>> getAll() {
        return ResponseEntity.ok(invoiceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getById(@PathVariable Long id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(invoiceRepository.findByCustomerId(customerId));
    }

    // ── MARK PAID ─────────────────────────────────────────────────────────────
    @PutMapping("/{id}/pay")
    public ResponseEntity<?> markPaid(@PathVariable Long id,
                                      @RequestParam(defaultValue = "Cash") String method) {
        return invoiceRepository.findById(id).map(inv -> {
            inv.setPaymentStatus("PAID");
            inv.setPaymentMethod(method);
            return ResponseEntity.ok(invoiceRepository.save(inv));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!invoiceRepository.existsById(id)) return ResponseEntity.notFound().build();
        invoiceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // ── PDF ───────────────────────────────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws IOException {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        byte[] pdf = pdfService.generateInvoicePdf(invoice);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .body(pdf);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private synchronized String generateInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        return "INV-" + String.format("%04d", count) + "-" + LocalDate.now().getYear();
    }
    private double nvl(Double d) { return d != null ? d : 0.0; }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    public static class InvoiceRequest {
        private Long   customerId;
        private List<ItemRequest> items;
        private Double discountAmt, taxPercent;
        private String notes, paymentMethod, technicianName;
        private Long   jobId;

        public Long   getCustomerId()               { return customerId; }
        public void   setCustomerId(Long v)         { customerId = v; }
        public List<ItemRequest> getItems()         { return items; }
        public void   setItems(List<ItemRequest> v) { items = v; }
        public Double getDiscountAmt()              { return discountAmt; }
        public void   setDiscountAmt(Double v)      { discountAmt = v; }
        public Double getTaxPercent()               { return taxPercent; }
        public void   setTaxPercent(Double v)       { taxPercent = v; }
        public String getNotes()                    { return notes; }
        public void   setNotes(String v)            { notes = v; }
        public String getPaymentMethod()            { return paymentMethod; }
        public void   setPaymentMethod(String v)    { paymentMethod = v; }
        public String getTechnicianName()           { return technicianName; }
        public void   setTechnicianName(String v)   { technicianName = v; }
        public Long   getJobId()                    { return jobId; }
        public void   setJobId(Long v)              { jobId = v; }
    }

    public static class ItemRequest {
        private String  serviceName, description;
        private Integer quantity;
        private Double  unitPrice;

        public String  getServiceName()         { return serviceName; }
        public void    setServiceName(String v) { serviceName = v; }
        public String  getDescription()         { return description; }
        public void    setDescription(String v) { description = v; }
        public Integer getQuantity()            { return quantity; }
        public void    setQuantity(Integer v)   { quantity = v; }
        public Double  getUnitPrice()           { return unitPrice; }
        public void    setUnitPrice(Double v)   { unitPrice = v; }
    }
}