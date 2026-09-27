package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.OrderRepository;
import com.Luxurycars.carstore.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final OrderRepository orderRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color GOLD = new Color(197, 160, 89);
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    @Autowired
    public InvoiceService(OrderRepository orderRepository,
                          CarRepository carRepository,
                          UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long orderId, Authentication authentication) {
        log.info("Generating PDF tax invoice for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOwnership(order, authentication);

        return generateInvoice(order);
    }

    public byte[] generateInvoice(Order order) {
        Car car = carRepository.findById(order.getCarId()).orElse(null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GOLD);
            Font smallMuted = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
            Font sectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GOLD);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, NAVY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
            Font whiteBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            // 1. Top Decorative Bar
            PdfPTable topBar = new PdfPTable(1);
            topBar.setWidthPercentage(100);
            PdfPCell barCell = new PdfPCell(new Phrase(""));
            barCell.setBackgroundColor(GOLD);
            barCell.setFixedHeight(4f);
            barCell.setBorder(Rectangle.NO_BORDER);
            topBar.addCell(barCell);
            document.add(topBar);

            document.add(new Paragraph(" "));

            // 2. Header: Company Info (Left) + Invoice Metadata (Right)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            // Company Info
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.addElement(new Paragraph("PROJECT LUXURY CARSTORE", brandFont));
            companyCell.addElement(new Paragraph("OFFICIAL AUTOMOTIVE TAX INVOICE", brandSubFont));
            companyCell.addElement(new Paragraph("GSTIN: 27AABCU9603R1ZM | CIN: U50100MH2024PTC123456", smallMuted));
            companyCell.addElement(new Paragraph("Showroom: Luxury Boulevard, Bandra-Worli Sea Link, Mumbai - 400050", smallMuted));
            companyCell.addElement(new Paragraph("Concierge: +91 22 6800 9000 | concierge@luxurycarstore.com", smallMuted));
            headerTable.addCell(companyCell);

            // Invoice Meta
            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String invoiceNumber = "INV-2026-" + String.format("%05d", order.getId());
            String orderDate = order.getOrderedAt() != null
                    ? order.getOrderedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                    : "N/A";

            Paragraph pInv = new Paragraph(invoiceNumber, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY));
            pInv.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pInv);

            Paragraph pDate = new Paragraph("Date: " + orderDate, smallMuted);
            pDate.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pDate);

            Paragraph pStatus = new Paragraph("Status: " + order.getStatus().name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GOLD));
            pStatus.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pStatus);

            Paragraph pPay = new Paragraph("Payment: " + order.getPaymentMethod(), smallMuted);
            pPay.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pPay);

            String txnRef = "TXN-2026-ORD" + order.getId() + "-" + Integer.toHexString(order.hashCode()).toUpperCase();
            Paragraph pTxn = new Paragraph("Ref: " + txnRef, smallMuted);
            pTxn.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pTxn);

            headerTable.addCell(metaCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // 3. Customer & Delivery Information (2-Column Box)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            // Bill To
            PdfPCell billToCell = new PdfPCell();
            billToCell.setBackgroundColor(LIGHT_BG);
            billToCell.setBorderColor(BORDER_COLOR);
            billToCell.setPadding(10f);
            billToCell.addElement(new Paragraph("CUSTOMER & CLIENT DETAILS", sectionTitle));
            billToCell.addElement(new Paragraph(order.getCustomerName(), boldFont));
            billToCell.addElement(new Paragraph("Email: " + order.getCustomerEmail(), regularFont));
            billToCell.addElement(new Paragraph("Phone: " + order.getCustomerPhone(), regularFont));
            infoTable.addCell(billToCell);

            // Ship To
            PdfPCell shipToCell = new PdfPCell();
            shipToCell.setBackgroundColor(LIGHT_BG);
            shipToCell.setBorderColor(BORDER_COLOR);
            shipToCell.setPadding(10f);
            shipToCell.addElement(new Paragraph("DESTINATION & SHOWROOM DETAILS", sectionTitle));
            String cityPin = (order.getDeliveryCity() != null ? order.getDeliveryCity() : "") +
                    (order.getDeliveryPincode() != null ? " - " + order.getDeliveryPincode() : "");
            shipToCell.addElement(new Paragraph("Delivery Address: " + order.getDeliveryAddress(), regularFont));
            shipToCell.addElement(new Paragraph("City: " + cityPin, regularFont));
            shipToCell.addElement(new Paragraph("Showroom Location: " + (car != null && car.getShowroomLocation() != null ? car.getShowroomLocation() : "Flagship Mumbai"), regularFont));
            infoTable.addCell(shipToCell);

            document.add(infoTable);

            document.add(new Paragraph(" "));

            // 4. Line Items Table
            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{8, 34, 18, 14, 12, 14});

            // Header cells
            String[] headers = {"#", "Vehicle Description", "Showroom", "Base Price", "Tax / Cess", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, whiteBold));
                cell.setBackgroundColor(NAVY);
                cell.setPadding(8f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                itemsTable.addCell(cell);
            }

            // Price Calculations
            BigDecimal totalAmount = order.getTotalAmount();
            // Calculate base and 18% tax
            BigDecimal divisor = new BigDecimal("1.18");
            BigDecimal basePrice = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = totalAmount.subtract(basePrice);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            // Row 1: Line Item
            PdfPCell c1 = new PdfPCell(new Phrase("1", regularFont));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);

            String vehicleDesc = order.getCarName() + (car != null && car.getColorOptions() != null ? "\nColor: " + car.getColorOptions() : "");
            PdfPCell c2 = new PdfPCell(new Phrase(vehicleDesc, regularFont));

            PdfPCell c3 = new PdfPCell(new Phrase(car != null && car.getShowroomLocation() != null ? car.getShowroomLocation() : "Mumbai", regularFont));
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell c4 = new PdfPCell(new Phrase(currencyFormat.format(basePrice), regularFont));
            c4.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell c5 = new PdfPCell(new Phrase(currencyFormat.format(taxAmount), regularFont));
            c5.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell c6 = new PdfPCell(new Phrase(currencyFormat.format(totalAmount), boldFont));
            c6.setHorizontalAlignment(Element.ALIGN_RIGHT);

            for (PdfPCell c : new PdfPCell[]{c1, c2, c3, c4, c5, c6}) {
                c.setPadding(8f);
                c.setBorderColor(BORDER_COLOR);
                itemsTable.addCell(c);
            }

            document.add(itemsTable);

            // 5. Totals Breakdown (Right-aligned table)
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(45);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{55, 45});

            addTotalRow(totalsTable, "Vehicle Base Outlay:", currencyFormat.format(basePrice), regularFont, regularFont);
            addTotalRow(totalsTable, "Luxury Goods GST (18%):", currencyFormat.format(taxAmount), regularFont, regularFont);
            addTotalRow(totalsTable, "Grand Total Outlay:", currencyFormat.format(totalAmount), boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GOLD));

            document.add(totalsTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // 6. Footer: Terms & Digital Stamp
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{60, 40});

            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(Rectangle.NO_BORDER);
            termsCell.addElement(new Paragraph("TERMS & CONDITIONS:", sectionTitle));
            termsCell.addElement(new Paragraph("1. Vehicle delivery subject to registration and insurance clearance.", smallMuted));
            termsCell.addElement(new Paragraph("2. Covered under 3-Year Concierge Assured Luxury Warranty.", smallMuted));
            termsCell.addElement(new Paragraph("3. In case of payment disputes, jurisdiction rests exclusively in Mumbai, India.", smallMuted));
            footerTable.addCell(termsCell);

            PdfPCell stampCell = new PdfPCell();
            stampCell.setBorder(Rectangle.NO_BORDER);
            stampCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pStamp1 = new Paragraph("For PROJECT LUXURY CARSTORE", boldFont);
            pStamp1.setAlignment(Element.ALIGN_RIGHT);
            stampCell.addElement(pStamp1);

            Paragraph pStamp2 = new Paragraph("[DIGITALLY VERIFIED DOCUMENT]", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GOLD));
            pStamp2.setAlignment(Element.ALIGN_RIGHT);
            stampCell.addElement(pStamp2);

            Paragraph pStamp3 = new Paragraph("Authorized Signatory\nTax & Compliance Cell", smallMuted);
            pStamp3.setAlignment(Element.ALIGN_RIGHT);
            stampCell.addElement(pStamp3);

            Paragraph pFoot = new Paragraph("This is an electronically generated tax invoice and does not require physical signature.", smallMuted);
            pFoot.setAlignment(Element.ALIGN_RIGHT);
            stampCell.addElement(pFoot);

            footerTable.addCell(stampCell);
            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("Failed to generate PDF invoice for order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage(), e);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
        cLabel.setPadding(5f);
        cLabel.setBorderColor(BORDER_COLOR);

        PdfPCell cVal = new PdfPCell(new Phrase(value, valFont));
        cVal.setPadding(5f);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cVal.setBorderColor(BORDER_COLOR);

        table.addCell(cLabel);
        table.addCell(cVal);
    }

    private void validateOwnership(Order order, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication required to access order invoice");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        if (isAdmin) {
            return;
        }

        String principal = authentication.getName();
        if (principal != null) {
            if (principal.equalsIgnoreCase(order.getCustomerEmail()) || principal.equalsIgnoreCase(order.getCustomerName())) {
                return;
            }
            AppUser user = userRepository.findByUsername(principal).orElse(null);
            if (user != null && user.getEmail() != null && user.getEmail().equalsIgnoreCase(order.getCustomerEmail())) {
                return;
            }
        }

        throw new AccessDeniedException("You are not authorized to view the invoice for order #" + order.getId());
    }
}
