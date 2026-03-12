package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Invoice;
import com.electrician.servicemanager.entity.Job;
import com.electrician.servicemanager.entity.User;
import com.electrician.servicemanager.repository.InvoiceRepository;
import com.electrician.servicemanager.repository.JobRepository;
import com.electrician.servicemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    private final InvoiceRepository invoiceRepository;
    private final JobRepository     jobRepository;
    private final UserRepository    userRepository;

    public StatsController(InvoiceRepository ir, JobRepository jr, UserRepository ur) {
        this.invoiceRepository = ir;
        this.jobRepository     = jr;
        this.userRepository    = ur;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(HttpServletRequest req) {
        User owner   = (User) req.getAttribute("currentUser");
        Long ownerId = owner.getId();

        LocalDate today      = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate yesterday  = today.minusDays(1);
        LocalDate dayBefore  = today.minusDays(2);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Invoice> allInvoicesFull = invoiceRepository.findAll();
        // Filter to this owner's invoices only (via customer.owner link)
        List<Invoice> allInvoices = allInvoicesFull.stream()
                .filter(i -> i.getCustomer() == null ||
                        i.getCustomer().getOwner() == null ||
                        ownerId.equals(i.getCustomer().getOwner().getId()))
                .collect(Collectors.toList());
        List<Job>     allJobs     = jobRepository.findJobsByOwner(ownerId);

        // ── Revenue by day helper ──────────────────────────────────────────
        double todayRev    = paidRevOn(allInvoices, today);
        double yesterdayRev= paidRevOn(allInvoices, yesterday);
        double dayBeforeRev= paidRevOn(allInvoices, dayBefore);
        double monthRevenue= paidRevBetween(allInvoices, monthStart, today);
        double totalRevenue= paidRevBetween(allInvoices, LocalDate.of(2000,1,1), today);
        double pending     = allInvoices.stream().filter(i->"UNPAID".equals(i.getPaymentStatus()))
                .mapToDouble(i->nvl(i.getTotalAmount())).sum();

        // ── Jobs stats ────────────────────────────────────────────────────
        long todayJobs     = allJobs.stream().filter(j->today.equals(j.getScheduledDate())).count();
        long activeJobs    = allJobs.stream().filter(j->!List.of("DONE","CANCELLED").contains(j.getStatus())).count();
        long completedJobs = allJobs.stream().filter(j->"DONE".equals(j.getStatus())).count();

        // ── 7-day revenue graph ───────────────────────────────────────────
        List<Map<String,Object>> revenueGraph = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String,Object> pt = new LinkedHashMap<>();
            pt.put("date",    d.format(DateTimeFormatter.ofPattern("dd MMM")));
            pt.put("revenue", Math.round(paidRevOn(allInvoices, d)));
            revenueGraph.add(pt);
        }

        // ── Technician stats with collection amounts ──────────────────────
        List<User> technicians = userRepository.findTechsByOwnerAndRole(ownerId, "TECHNICIAN");
        List<Map<String,Object>> techStats = technicians.stream().map(tech -> {
                    // Jobs assigned to this tech
                    List<Job> techJobs = allJobs.stream()
                            .filter(j -> j.getTechnician()!=null && j.getTechnician().getId().equals(tech.getId()))
                            .collect(Collectors.toList());

                    // Invoices created by this tech (technicianName matches)
                    List<Invoice> techInvoices = allInvoices.stream()
                            .filter(i -> tech.getName().equals(i.getTechnicianName()))
                            .collect(Collectors.toList());

                    double todayCollection  = techInvoices.stream()
                            .filter(i->"PAID".equals(i.getPaymentStatus()) && today.equals(i.getInvoiceDate()))
                            .mapToDouble(i->nvl(i.getTotalAmount())).sum();
                    double yestCollection   = techInvoices.stream()
                            .filter(i->"PAID".equals(i.getPaymentStatus()) && yesterday.equals(i.getInvoiceDate()))
                            .mapToDouble(i->nvl(i.getTotalAmount())).sum();
                    double monthCollection  = techInvoices.stream()
                            .filter(i->"PAID".equals(i.getPaymentStatus()) && !i.getInvoiceDate().isBefore(monthStart))
                            .mapToDouble(i->nvl(i.getTotalAmount())).sum();
                    double pendingCollection= techInvoices.stream()
                            .filter(i->"UNPAID".equals(i.getPaymentStatus()))
                            .mapToDouble(i->nvl(i.getTotalAmount())).sum();

                    Map<String,Object> t = new LinkedHashMap<>();
                    t.put("id",               tech.getId());
                    t.put("name",             tech.getName());
                    t.put("mobile",           tech.getMobile());
                    t.put("isActive",         tech.getIsActive());
                    t.put("activeStartedAt",  tech.getActiveStartedAt() != null
                            ? tech.getActiveStartedAt().toString() : null); // ISO string for JS
                    t.put("totalJobs",        techJobs.size());
                    // doneJobs per period — completedAt ya scheduledDate se filter
                    t.put("doneJobs",         techJobs.stream().filter(j->"DONE".equals(j.getStatus())).count());
                    t.put("todayDoneJobs",    techJobs.stream().filter(j->"DONE".equals(j.getStatus())
                            && today.equals(j.getScheduledDate())).count());
                    t.put("yestDoneJobs",     techJobs.stream().filter(j->"DONE".equals(j.getStatus())
                            && yesterday.equals(j.getScheduledDate())).count());
                    t.put("weekDoneJobs",     techJobs.stream().filter(j->"DONE".equals(j.getStatus())
                            && j.getScheduledDate()!=null && !j.getScheduledDate().isBefore(today.minusDays(6))).count());
                    t.put("monthDoneJobs",    techJobs.stream().filter(j->"DONE".equals(j.getStatus())
                            && j.getScheduledDate()!=null && !j.getScheduledDate().isBefore(monthStart)).count());
                    t.put("activeJobs",       techJobs.stream().filter(j->!List.of("DONE","CANCELLED").contains(j.getStatus())).count());
                    t.put("monthJobs",        techJobs.stream().filter(j->j.getScheduledDate()!=null&&!j.getScheduledDate().isBefore(monthStart)).count());
                    t.put("todayCollection",  Math.round(todayCollection));
                    t.put("yestCollection",   Math.round(yestCollection));
                    t.put("monthCollection",  Math.round(monthCollection));
                    t.put("pendingCollection",Math.round(pendingCollection));
                    t.put("totalInvoices",    techInvoices.size());
                    return t;
                }).sorted((a,b)->Long.compare((long)b.get("monthCollection"),(long)a.get("monthCollection")))
                .collect(Collectors.toList());

        // ── Top machines ──────────────────────────────────────────────────
        Map<String,Long> machineCount = allJobs.stream()
                .filter(j->j.getMachineType()!=null)
                .collect(Collectors.groupingBy(Job::getMachineType, Collectors.counting()));
        List<Map<String,Object>> topMachines = machineCount.entrySet().stream()
                .sorted((a,b)->Long.compare(b.getValue(),a.getValue())).limit(5)
                .map(e->{ Map<String,Object> m=new LinkedHashMap<>(); m.put("type",e.getKey()); m.put("count",e.getValue()); return m; })
                .collect(Collectors.toList());

        // ── Unpaid invoices (recent 5) ─────────────────────────────────────
        List<Map<String,Object>> unpaidList = allInvoices.stream()
                .filter(i->"UNPAID".equals(i.getPaymentStatus()))
                .sorted((a,b)->b.getInvoiceDate().compareTo(a.getInvoiceDate()))
                .limit(5)
                .map(i->{ Map<String,Object> m=new LinkedHashMap<>();
                    m.put("id",            i.getId());
                    m.put("invoiceNumber", i.getInvoiceNumber());
                    m.put("customerName",  i.getCustomer()!=null?i.getCustomer().getName():"Unknown");
                    m.put("amount",        Math.round(nvl(i.getTotalAmount())));
                    m.put("date",          i.getInvoiceDate().toString());
                    return m; })
                .collect(Collectors.toList());

        // ── Response ──────────────────────────────────────────────────────
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("todayRevenue",    Math.round(todayRev));
        response.put("yesterdayRevenue",Math.round(yesterdayRev));
        response.put("dayBeforeRevenue",Math.round(dayBeforeRev));
        response.put("monthRevenue",    Math.round(monthRevenue));
        response.put("totalRevenue",    Math.round(totalRevenue));
        response.put("pendingAmount",   Math.round(pending));
        response.put("todayJobs",       todayJobs);
        response.put("activeJobs",      activeJobs);
        response.put("completedJobs",   completedJobs);
        response.put("totalJobs",       allJobs.size());
        response.put("revenueGraph",    revenueGraph);
        response.put("technicianStats", techStats);
        response.put("topMachines",     topMachines);
        response.put("unpaidInvoices",  unpaidList);
        response.put("totalInvoices",   allInvoices.size());
        response.put("paidInvoices",    allInvoices.stream().filter(i->"PAID".equals(i.getPaymentStatus())).count());
        return ResponseEntity.ok(response);
    }

    private double paidRevOn(List<Invoice> invs, LocalDate date) {
        return invs.stream()
                .filter(i->"PAID".equals(i.getPaymentStatus()) && date.equals(i.getInvoiceDate()))
                .mapToDouble(i->nvl(i.getTotalAmount())).sum();
    }
    private double paidRevBetween(List<Invoice> invs, LocalDate from, LocalDate to) {
        return invs.stream()
                .filter(i->"PAID".equals(i.getPaymentStatus())
                        && i.getInvoiceDate()!=null
                        && !i.getInvoiceDate().isBefore(from)
                        && !i.getInvoiceDate().isAfter(to))
                .mapToDouble(i->nvl(i.getTotalAmount())).sum();
    }
    private double nvl(Double d) { return d!=null?d:0.0; }
}