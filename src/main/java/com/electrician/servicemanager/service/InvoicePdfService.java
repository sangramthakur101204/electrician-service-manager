package com.electrician.servicemanager.service;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.Invoice;
import com.electrician.servicemanager.entity.InvoiceItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    // ── Business Info ─────────────────────────────────────────────
    private static final String BUSINESS_NAME    = "MATOSHREE ENTERPRISES";
    private static final String BUSINESS_OWNER   = "Rushikesh Rajput";
    private static final String BUSINESS_PHONE   = "+91 9172730502 / 9834796734";
    private static final String BUSINESS_EMAIL   = "rushirajput941@gmail.com";
    private static final String BUSINESS_TAGLINE = "Washing Machine | Microwave | Water Purifier | Refrigerator";

    // ── Brand Colors ──────────────────────────────────────────────
    private static final DeviceRgb PRIMARY     = new DeviceRgb(30, 64, 175);   // Deep blue
    private static final DeviceRgb PRIMARY_LIGHT = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb ACCENT      = new DeviceRgb(245, 158, 11);  // Amber/gold
    private static final DeviceRgb TEXT_DARK   = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb TEXT_GRAY   = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb BG_LIGHT    = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb GREEN       = new DeviceRgb(22, 163, 74);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateInvoicePdf(Invoice invoice) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 40, 30, 40);

        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        Customer c = invoice.getCustomer();

        // ═══════════════════════════════════════════════════
        // 1. HEADER — Logo Area + Invoice Title
        // ═══════════════════════════════════════════════════
        Table header = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                .setWidth(UnitValue.createPercentValue(100));

        // Left: Business Name & Info
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0);

        leftCell.add(new Paragraph(BUSINESS_NAME)
                .setFont(bold).setFontSize(18).setFontColor(PRIMARY)
                .setMarginBottom(2));
        leftCell.add(new Paragraph(BUSINESS_TAGLINE)
                .setFont(regular).setFontSize(9).setFontColor(TEXT_GRAY)
                .setMarginBottom(4));
        leftCell.add(new Paragraph("Ph: " + BUSINESS_PHONE + "   ✉ " + BUSINESS_EMAIL)
                .setFont(regular).setFontSize(9).setFontColor(TEXT_DARK));

        // Right: Invoice Badge
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(0);

        // Invoice box
        Table invoiceBadge = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT);
        Cell badge = new Cell()
                .setBackgroundColor(PRIMARY)
                .setPadding(10)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        badge.add(new Paragraph("INVOICE").setFont(bold).setFontSize(16)
                .setFontColor(ColorConstants.WHITE).setMarginBottom(2));
        badge.add(new Paragraph(invoice.getInvoiceNumber()).setFont(bold).setFontSize(11)
                .setFontColor(ACCENT).setMarginBottom(4));
        badge.add(new Paragraph("Date: " + (invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(FMT) : "—"))
                .setFont(regular).setFontSize(8).setFontColor(ColorConstants.WHITE));
        if (invoice.getDueDate() != null) {
            badge.add(new Paragraph("Due: " + invoice.getDueDate().format(FMT))
                    .setFont(regular).setFontSize(8).setFontColor(ColorConstants.WHITE));
        }
        invoiceBadge.addCell(badge);
        rightCell.add(invoiceBadge);

        header.addCell(leftCell);
        header.addCell(rightCell);
        doc.add(header);

        // Separator line
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setStrokeColor(PRIMARY).setMarginTop(8).setMarginBottom(12));

        // ═══════════════════════════════════════════════════
        // 2. BILL TO + MACHINE INFO
        // ═══════════════════════════════════════════════════
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(16);

        // Bill To
        Cell billTo = new Cell().setBorder(Border.NO_BORDER);
        billTo.add(sectionTitle("BILL TO", bold));
        billTo.add(infoLine("Name    :", c.getName(), regular, bold));
        billTo.add(infoLine("Mobile  :", "+91 " + c.getMobile(), regular, bold));
        if (c.getAddress() != null && !c.getAddress().isBlank())
            billTo.add(infoLine("Address :", c.getAddress(), regular, bold));

        // Machine Info
        Cell machineInfo = new Cell().setBorder(Border.NO_BORDER);
        machineInfo.add(sectionTitle("MACHINE DETAILS", bold));
        machineInfo.add(infoLine("Type    :", c.getMachineType(), regular, bold));
        machineInfo.add(infoLine("Brand   :", c.getMachineBrand(), regular, bold));
        if (c.getModel() != null && !c.getModel().isBlank())
            machineInfo.add(infoLine("Model   :", c.getModel(), regular, bold));
        if (c.getSerialNumber() != null && !c.getSerialNumber().isBlank())
            machineInfo.add(infoLine("Serial  :", c.getSerialNumber(), regular, bold));
        if (c.getServiceDate() != null)
            machineInfo.add(infoLine("Service :", c.getServiceDate().format(FMT), regular, bold));

        infoTable.addCell(billTo);
        infoTable.addCell(machineInfo);
        doc.add(infoTable);

        // ═══════════════════════════════════════════════════
        // 3. ITEMS TABLE
        // ═══════════════════════════════════════════════════
        doc.add(sectionTitle("SERVICES / ITEMS", bold));

        Table itemTable = new Table(UnitValue.createPercentArray(new float[]{5, 38, 25, 10, 12, 10}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(4).setMarginBottom(12);

        // Table header row
        String[] colHeaders = {"#", "Service / Description", "Details", "Qty", "Unit Price", "Total"};
        for (String h : colHeaders) {
            itemTable.addHeaderCell(new Cell()
                    .setBackgroundColor(PRIMARY)
                    .setPadding(7)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph(h)
                            .setFont(bold).setFontSize(9)
                            .setFontColor(ColorConstants.WHITE)
                            .setTextAlignment(TextAlignment.CENTER)));
        }

        // Items rows
        int rowNum = 1;
        for (InvoiceItem item : invoice.getItems()) {
            boolean even = rowNum % 2 == 0;
            com.itextpdf.kernel.colors.Color rowBg = even ? BG_LIGHT : ColorConstants.WHITE;

            itemTable.addCell(itemCell(String.valueOf(rowNum++), rowBg, regular, TextAlignment.CENTER));
            itemTable.addCell(itemCell(nvl(item.getServiceName()), rowBg, bold, TextAlignment.LEFT));
            itemTable.addCell(itemCell(nvl(item.getDescription()), rowBg, regular, TextAlignment.LEFT));
            itemTable.addCell(itemCell(String.valueOf(item.getQuantity()), rowBg, regular, TextAlignment.CENTER));
            itemTable.addCell(itemCell("Rs." + fmt(item.getUnitPrice()), rowBg, regular, TextAlignment.RIGHT));
            itemTable.addCell(itemCell("Rs." + fmt(item.getTotalPrice()), rowBg, bold, TextAlignment.RIGHT));
        }
        doc.add(itemTable);

        // ═══════════════════════════════════════════════════
        // 4. TOTALS
        // ═══════════════════════════════════════════════════
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(16);

        // Left: Notes
        Cell notesCell = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4);
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            notesCell.add(sectionTitle("NOTES", bold));
            notesCell.add(new Paragraph(invoice.getNotes())
                    .setFont(regular).setFontSize(9).setFontColor(TEXT_GRAY));
        }

        // Right: Amount summary
        Cell amtCell = new Cell()
                .setBorder(new SolidBorder(PRIMARY_LIGHT, 1))
                .setPadding(12)
                .setBackgroundColor(BG_LIGHT);

        amtCell.add(amtRow("Subtotal", "Rs." + fmt(invoice.getSubtotal()), regular));
        if (invoice.getDiscountAmt() != null && invoice.getDiscountAmt() > 0)
            amtCell.add(amtRow("Discount (-)", "Rs." + fmt(invoice.getDiscountAmt()), regular));
        if (invoice.getTaxPercent() != null && invoice.getTaxPercent() > 0)
            amtCell.add(amtRow("GST (" + invoice.getTaxPercent().intValue() + "%)",
                    "Rs." + fmt(invoice.getTaxAmount()), regular));

        // Separator in totals
        amtCell.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setStrokeColor(TEXT_GRAY).setMarginTop(4).setMarginBottom(4));

        // TOTAL row
        Table totalRow = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));
        totalRow.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("TOTAL AMOUNT").setFont(bold).setFontSize(11).setFontColor(PRIMARY)));
        totalRow.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Rs." + fmt(invoice.getTotalAmount()))
                        .setFont(bold).setFontSize(14).setFontColor(PRIMARY)));
        amtCell.add(totalRow);

        // Payment status badge
        String status = invoice.getPaymentStatus() != null ? invoice.getPaymentStatus() : "UNPAID";
        DeviceRgb statusColor = "PAID".equalsIgnoreCase(status) ? GREEN : ACCENT;
        amtCell.add(new Paragraph("Payment: " + status)
                .setFont(bold).setFontSize(10).setFontColor(statusColor)
                .setMarginTop(6).setTextAlignment(TextAlignment.CENTER));

        totalsTable.addCell(notesCell);
        totalsTable.addCell(amtCell);
        doc.add(totalsTable);

        // ═══════════════════════════════════════════════════
        // 5. WARRANTY SECTION (if warranty exists)
        // ═══════════════════════════════════════════════════
        if (c.getWarrantyEnd() != null) {
            Table warrantyBox = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(12);
            Cell wCell = new Cell()
                    .setBackgroundColor(PRIMARY_LIGHT)
                    .setBorder(new SolidBorder(PRIMARY, 1))
                    .setPadding(10);
            wCell.add(new Paragraph("WARRANTY INFORMATION")
                    .setFont(bold).setFontSize(10).setFontColor(PRIMARY).setMarginBottom(4));
            wCell.add(new Paragraph(
                    "Warranty Period: " + nvl(c.getWarrantyPeriod()) +
                            "   |   Valid From: " + (c.getServiceDate() != null ? c.getServiceDate().format(FMT) : "—") +
                            "   |   Valid Till: " + c.getWarrantyEnd().format(FMT))
                    .setFont(regular).setFontSize(9).setFontColor(TEXT_DARK));
            warrantyBox.addCell(wCell);
            doc.add(warrantyBox);
        }

        // ═══════════════════════════════════════════════════
        // 6. FOOTER
        // ═══════════════════════════════════════════════════
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setStrokeColor(PRIMARY).setMarginBottom(6));

        Table footer = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell fLeft = new Cell().setBorder(Border.NO_BORDER);
        fLeft.add(new Paragraph("Thank you for choosing " + BUSINESS_NAME + "!")
                .setFont(bold).setFontSize(9).setFontColor(PRIMARY));
        fLeft.add(new Paragraph("For any queries: " + BUSINESS_PHONE + " | " + BUSINESS_EMAIL)
                .setFont(regular).setFontSize(8).setFontColor(TEXT_GRAY));

        Cell fRight = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        fRight.add(new Paragraph("Authorised Signature")
                .setFont(regular).setFontSize(8).setFontColor(TEXT_GRAY));
        fRight.add(new Paragraph("\n\n_____________________")
                .setFont(regular).setFontSize(9).setFontColor(TEXT_DARK));
        fRight.add(new Paragraph(BUSINESS_NAME)
                .setFont(bold).setFontSize(8).setFontColor(TEXT_DARK));

        footer.addCell(fLeft);
        footer.addCell(fRight);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Paragraph sectionTitle(String title, PdfFont bold) {
        return new Paragraph(title)
                .setFont(bold).setFontSize(9).setFontColor(TEXT_GRAY)
                .setMarginBottom(3);
    }

    private Paragraph infoLine(String label, String value, PdfFont regular, PdfFont bold) {
        return new Paragraph()
                .add(new Text(label + "  ").setFont(regular).setFontSize(9).setFontColor(TEXT_GRAY))
                .add(new Text(value).setFont(bold).setFontSize(9).setFontColor(TEXT_DARK))
                .setMarginBottom(2);
    }

    private Cell itemCell(String text, com.itextpdf.kernel.colors.Color bg, PdfFont font, TextAlignment align) {
        return new Cell()
                .setBackgroundColor(bg)
                .setPadding(6)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(PRIMARY_LIGHT, 0.5f))
                .add(new Paragraph(text).setFont(font).setFontSize(9)
                        .setFontColor(TEXT_DARK).setTextAlignment(align));
    }

    private Paragraph amtRow(String label, String value, PdfFont font) {
        return new Paragraph()
                .add(new Text(label + ":  ").setFont(font).setFontSize(9).setFontColor(TEXT_GRAY))
                .add(new Text(value).setFont(font).setFontSize(9).setFontColor(TEXT_DARK))
                .setMarginBottom(3);
    }

    private String fmt(Double d) {
        if (d == null) return "0.00";
        return String.format("%.2f", d);
    }

    private String nvl(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }
}