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

    public PaymentIntentResponseDTO createPaymentIntent(PaymentIntentRequestDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order #ORD-" + order.getId() + " is already paid and confirmed.");
        }

        String intentId = "pi_lux_" + UUID.randomUUID().toString().substring(0, 12);
        String clientSecret = "sec_lux_" + UUID.randomUUID().toString();

        log.info("💳 Initialized payment intent {} for Order #ORD-{} with amount {}",
                intentId, order.getId(), order.getTotalAmount());

        return PaymentIntentResponseDTO.builder()
                .paymentIntentId(intentId)
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .clientSecret(clientSecret)
                .status("REQUIRES_PAYMENT")
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
                order.setPaymentMethod("CARD (Verified via Payment Gateway)");
                Order updated = orderRepository.save(order);

                log.info("✅ Order #ORD-{} marked as CONFIRMED via Payment Webhook", updated.getId());

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
