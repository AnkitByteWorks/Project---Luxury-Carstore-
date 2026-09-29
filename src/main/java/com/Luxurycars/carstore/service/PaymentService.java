package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderMapper;
import com.Luxurycars.carstore.dto.PaymentIntentRequestDTO;
import com.Luxurycars.carstore.dto.PaymentIntentResponseDTO;
import com.Luxurycars.carstore.dto.PaymentWebhookRequestDTO;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final OrderEventService orderEventService;
    private final InvoiceService invoiceService;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public PaymentService(OrderRepository orderRepository,
                          @Autowired(required = false) OrderEventService orderEventService,
                          @Autowired(required = false) InvoiceService invoiceService,
                          @Autowired(required = false) EmailNotificationService emailNotificationService) {
        this.orderRepository = orderRepository;
        this.orderEventService = orderEventService;
        this.invoiceService = invoiceService;
        this.emailNotificationService = emailNotificationService;
    }

    public PaymentIntentResponseDTO createPaymentIntent(Long orderId, String paymentMethod) {
        return createPaymentIntent(PaymentIntentRequestDTO.builder()
                .orderId(orderId)
                .paymentMethod(paymentMethod != null ? paymentMethod : "UPI")
                .build());
    }

    public PaymentIntentResponseDTO createPaymentIntent(PaymentIntentRequestDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order #ORD-" + order.getId() + " is already paid and confirmed.");
        }

        String method = (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isBlank())
                ? dto.getPaymentMethod().trim()
                : "CARD";

        String intentId = (method.equalsIgnoreCase("UPI") ? "upi_" : "pi_lux_")
                + UUID.randomUUID().toString().substring(0, 12);
        String clientSecret = "sec_lux_" + UUID.randomUUID().toString();

        String upiString = null;
        if ("UPI".equalsIgnoreCase(method)) {
            // Dynamic UPI payment string: upi://pay?pa=carstore.bespoke@icici&pn=CarstoreVIP&am={orderTotal}&tr={orderId}&cu=INR
            upiString = "upi://pay?pa=carstore.bespoke@icici&pn=CarstoreVIP&am="
                    + order.getTotalAmount().toPlainString()
                    + "&tr=" + order.getId()
                    + "&cu=INR";

            // Record preferred payment method on order
            order.setPaymentMethod("UPI");
            orderRepository.save(order);
        }

        log.info("💳 Initialized payment intent {} ({}) for Order #ORD-{} with amount {}",
                intentId, method, order.getId(), order.getTotalAmount());

        return PaymentIntentResponseDTO.builder()
                .paymentIntentId(intentId)
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .clientSecret(clientSecret)
                .status("REQUIRES_PAYMENT")
                .paymentMethod(method.toUpperCase())
                .upiString(upiString)
                .checkoutUrl("/checkout/pay?intent=" + intentId + "&order=" + order.getId())
                .build();
    }

    @Transactional
    public boolean processWebhook(PaymentWebhookRequestDTO dto) {
        log.info("🔔 Processing payment webhook event: {} for order ID: {}",
                dto.getEventType(), dto.getOrderId());

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        if ("payment_intent.succeeded".equalsIgnoreCase(dto.getEventType())) {
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.CONFIRMED);

                boolean isUpi = (order.getPaymentMethod() != null && order.getPaymentMethod().toUpperCase().contains("UPI"))
                        || (dto.getPaymentIntentId() != null && dto.getPaymentIntentId().toLowerCase().startsWith("upi"));
                order.setPaymentMethod(isUpi
                        ? "UPI (Verified via Payment Gateway)"
                        : "CARD (Verified via Payment Gateway)");

                Order updated = orderRepository.save(order);

                log.info("✅ Order #ORD-{} marked as CONFIRMED via Payment Webhook ({})",
                        updated.getId(), updated.getPaymentMethod());

                // Broadcast live real-time update to connected frontend subscribers
                if (orderEventService != null) {
                    orderEventService.publishOrderEvent(
                            updated.getId(),
                            OrderMapper.toResponseDTO(updated)
                    );
                }

                // Asynchronously generate luxury PDF invoice and dispatch confirmation email
                if (emailNotificationService != null) {
                    byte[] pdf = null;
                    if (invoiceService != null) {
                        try {
                            pdf = invoiceService.generateInvoice(updated);
                        } catch (Exception ex) {
                            log.error("Failed to generate invoice for webhook order: {}", ex.getMessage());
                        }
                    }
                    emailNotificationService.sendOrderConfirmationEmail(updated, pdf);
                }
            }
            return true;
        } else if ("payment_intent.payment_failed".equalsIgnoreCase(dto.getEventType())) {
            log.warn("⚠️ Payment intent failed for Order #ORD-{}", order.getId());
            if (orderEventService != null) {
                orderEventService.publishOrderEvent(
                        order.getId(),
                        OrderMapper.toResponseDTO(order)
                );
            }
            return false;
        }

        return false;
    }
}
