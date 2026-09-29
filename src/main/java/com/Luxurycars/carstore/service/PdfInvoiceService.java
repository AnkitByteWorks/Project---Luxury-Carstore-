package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service for PDF tax invoice generation.
 * Supports standard orders as well as Bespoke Customization Specs.
 */
@Service
public class PdfInvoiceService {

    private final InvoiceService invoiceService;

    @Autowired
    public PdfInvoiceService(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public byte[] generateInvoice(Order order) {
        return invoiceService.generateInvoice(order);
    }

    public byte[] generateInvoicePdf(Long orderId, Authentication authentication) {
        return invoiceService.generateInvoicePdf(orderId, authentication);
    }
}
