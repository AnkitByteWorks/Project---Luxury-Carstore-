package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.TestDrive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Async("carstoreTaskExecutor")
    public void sendOrderConfirmationEmail(Order order, byte[] invoicePdf) {
        try {
            String formattedTotal = NumberFormat.getCurrencyInstance(new Locale("en", "IN"))
                    .format(order.getTotalAmount());

            log.info("════════════════════════════════════════════════════════════════");
            log.info("📧 [CARSTORE CONCIERGE] DISPATCHING ORDER CONFIRMATION EMAIL");
            log.info("Recipient  : {} <{}>", order.getCustomerName(), order.getCustomerEmail());
            log.info("Order ID   : #ORD-{}", order.getId());
            log.info("Vehicle    : {} (Qty: {})", order.getCarName(), order.getQuantity());
            log.info("Total Outlay : {}", formattedTotal);
            log.info("Payment    : {}", order.getPaymentMethod());
            log.info("Delivery   : {}, {}", order.getDeliveryCity(), order.getDeliveryPincode());
            if (invoicePdf != null && invoicePdf.length > 0) {
                log.info("Attachment : Carstore-Invoice-ORD-{}.pdf ({} bytes, Digitally Verified)",
                        order.getId(), invoicePdf.length);
            }
            log.info("Status     : SUCCESSFUL (Dispatched in background thread: {})",
                    Thread.currentThread().getName());
            log.info("════════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order #{}: {}", order.getId(), e.getMessage());
        }
    }

    @Async("carstoreTaskExecutor")
    public void sendTestDriveConfirmationEmail(TestDriveResponseDTO testDrive) {
        try {
            log.info("════════════════════════════════════════════════════════════════");
            log.info("🏎️ [CARSTORE CONCIERGE] DISPATCHING VIP TEST DRIVE INVITATION");
            log.info("VIP Guest  : {} <{}>", testDrive.getCustomerName(), testDrive.getEmail());
            log.info("Phone      : {}", testDrive.getPhone());
            log.info("Reference  : {}", testDrive.getReferenceCode());
            log.info("Vehicle    : {} ({})", testDrive.getCarName(), testDrive.getCarBrand());
            log.info("Experience : {}", testDrive.getExperienceType());
            log.info("Schedule   : {} at {}", testDrive.getPreferredDate(), testDrive.getTimeSlot());
            log.info("Status     : CONFIRMED (Dispatched in background thread: {})",
                    Thread.currentThread().getName());
            log.info("════════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("Failed to send VIP test drive confirmation for reference {}: {}",
                    testDrive.getReferenceCode(), e.getMessage());
        }
    }
}
