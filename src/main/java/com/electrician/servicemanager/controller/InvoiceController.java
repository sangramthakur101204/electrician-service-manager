package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.Invoice;
import com.electrician.servicemanager.entity.InvoiceItem;
import com.electrician.servicemanager.repository.CustomerRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoicePdfService pdfService;

    public InvoiceController(InvoiceRepository invoiceRepository,
                             CustomerRepository customerRepository,
                             InvoicePdfService pdfService) {
        this.invoiceRepository  = invoiceRepository;
        this.customerRepository = customerRepository;
        this.pdfService         = pdfService;
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody InvoiceRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + req.getCustomerId()));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(7));
        invoice.setPaymentStatus(req.getPaymentStatus() != null ? req.getPaymentStatus() : "UNPAID"); // ← UPDATED
        invoice.setPaymentMethod(req.getPaymentMethod());
        invoice.setNotes(req.getNotes());
        invoice.setDiscountAmt(nvl(req.getDiscountAmt()));
        invoice.setTaxPercent(nvl(req.getTaxPercent()));

        Invoice saved = invoiceRepository.save(invoice);

        List<InvoiceItem> items = req.getItems().stream().map(i -> {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(saved);
            item.setServiceName(i.getServiceName());
            item.setDescription(i.getDescription());
            item.setQuantity(i.getQuantity() != null ? i.getQuantity() : 1);
            item.setUnitPrice(nvl(i.getUnitPrice()));
            item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
            return item;
        }).collect(Collectors.toCollection(ArrayList::new)); // ← UPDATED (was .toList())

        double subtotal = 0;
        for (InvoiceItem item : items) subtotal += item.getTotalPrice();
        double discount  = nvl(req.getDiscountAmt());
        double taxPct    = nvl(req.getTaxPercent());
        double taxAmount = (subtotal - discount) * taxPct / 100.0;
        double total     = subtotal - discount + taxAmount;

        saved.setItems(items);
        saved.setSubtotal(subtotal);
        saved.setTaxAmount(taxAmount);
        saved.setTotalAmount(total);

        return ResponseEntity.ok(invoiceRepository.save(saved));
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> getAll() {
        return ResponseEntity.ok(invoiceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getOne(@PathVariable Long id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(invoiceRepository.findByCustomerId(customerId));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Invoice> markPaid(@PathVariable Long id,
                                            @RequestParam(defaultValue = "Cash") String method) {
        return invoiceRepository.findById(id).map(inv -> {
            inv.setPaymentStatus("PAID");
            inv.setPaymentMethod(method);
            return ResponseEntity.ok(invoiceRepository.save(inv));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!invoiceRepository.existsById(id)) return ResponseEntity.notFound().build();
        invoiceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws IOException {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));

        byte[] pdf = pdfService.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .body(pdf);
    }

    private String generateInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        int year = LocalDate.now().getYear();
        return "INV-" + String.format("%04d", count) + "-" + year;
    }

    private double nvl(Double d) { return d != null ? d : 0.0; }

    public static class InvoiceRequest {
        private Long customerId;
        private List<ItemRequest> items;
        private Double discountAmt;
        private Double taxPercent;
        private String notes;
        private String paymentMethod;
        private String paymentStatus; // ← NAYA FIELD

        public Long getCustomerId()              { return customerId; }
        public void setCustomerId(Long v)        { customerId = v; }
        public List<ItemRequest> getItems()      { return items; }
        public void setItems(List<ItemRequest> v){ items = v; }
        public Double getDiscountAmt()           { return discountAmt; }
        public void setDiscountAmt(Double v)     { discountAmt = v; }
        public Double getTaxPercent()            { return taxPercent; }
        public void setTaxPercent(Double v)      { taxPercent = v; }
        public String getNotes()                 { return notes; }
        public void setNotes(String v)           { notes = v; }
        public String getPaymentMethod()         { return paymentMethod; }
        public void setPaymentMethod(String v)   { paymentMethod = v; }
        public String getPaymentStatus()         { return paymentStatus; } // ← NAYA
        public void setPaymentStatus(String v)   { paymentStatus = v; }   // ← NAYA
    }

    public static class ItemRequest {
        private String serviceName;
        private String description;
        private Integer quantity;
        private Double unitPrice;

        public String getServiceName()        { return serviceName; }
        public void setServiceName(String v)  { serviceName = v; }
        public String getDescription()        { return description; }
        public void setDescription(String v)  { description = v; }
        public Integer getQuantity()          { return quantity; }
        public void setQuantity(Integer v)    { quantity = v; }
        public Double getUnitPrice()          { return unitPrice; }
        public void setUnitPrice(Double v)    { unitPrice = v; }
    }
}