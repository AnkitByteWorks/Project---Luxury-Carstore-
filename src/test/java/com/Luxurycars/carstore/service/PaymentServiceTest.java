package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.dto.PaymentIntentRequestDTO;
import com.Luxurycars.carstore.dto.PaymentIntentResponseDTO;
import com.Luxurycars.carstore.dto.PaymentWebhookRequestDTO;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventService orderEventService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private EmailNotificationService emailNotificationService;

    private PaymentService paymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(orderRepository, orderEventService, invoiceService, emailNotificationService);
        testOrder = Order.builder()
                .id(1L)
                .carId(5L)
                .carName("Bugatti Chiron")
                .unitPrice(new BigDecimal("400000000.00"))
                .quantity(1)
                .totalAmount(new BigDecimal("400000000.00"))
                .customerName("Bruce Wayne")
                .customerEmail("bruce@wayne.com")
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("createPaymentIntent - should generate intent details successfully")
    void createPaymentIntent_shouldCreateIntent() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        PaymentIntentRequestDTO request = PaymentIntentRequestDTO.builder()
                .orderId(1L)
                .paymentMethod("CARD")
                .build();

        PaymentIntentResponseDTO response = paymentService.createPaymentIntent(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo("400000000.00");
        assertThat(response.getPaymentIntentId()).startsWith("pi_lux_");
        assertThat(response.getStatus()).isEqualTo("REQUIRES_PAYMENT");
    }

    @Test
    @DisplayName("createPaymentIntent - should throw when order already confirmed")
    void createPaymentIntent_shouldThrow_whenOrderConfirmed() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        PaymentIntentRequestDTO request = PaymentIntentRequestDTO.builder()
                .orderId(1L)
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentIntent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("processWebhook - should confirm order on payment_intent.succeeded")
    void processWebhook_shouldConfirmOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentWebhookRequestDTO webhook = PaymentWebhookRequestDTO.builder()
                .orderId(1L)
                .paymentIntentId("pi_lux_12345")
                .eventType("payment_intent.succeeded")
                .build();

        boolean processed = paymentService.processWebhook(webhook);

        assertThat(processed).isTrue();
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderEventService, times(1)).publishOrderEvent(eq(1L), any(OrderResponseDTO.class));
        verify(emailNotificationService, times(1)).sendOrderConfirmationEmail(eq(testOrder), any());
    }

    @Test
    @DisplayName("createPaymentIntent - should support UPI and generate dynamic UPI string")
    void createPaymentIntent_shouldSupportUpiAndGenerateDynamicUpiString() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        PaymentIntentResponseDTO response = paymentService.createPaymentIntent(1L, "UPI");

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getPaymentMethod()).isEqualTo("UPI");
        assertThat(response.getUpiString()).isNotNull();
        assertThat(response.getUpiString()).isEqualTo(
                "upi://pay?pa=carstore.bespoke@icici&pn=CarstoreVIP&am=400000000.00&tr=1&cu=INR"
        );
        assertThat(response.getUpiPayload()).isEqualTo(response.getUpiString());
    }

    @Test
    @DisplayName("processWebhook - should confirm order and record UPI payment method")
    void processWebhook_shouldConfirmOrder_forUpiPayment() {
        testOrder.setPaymentMethod("UPI");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentWebhookRequestDTO webhook = PaymentWebhookRequestDTO.builder()
                .orderId(1L)
                .paymentIntentId("upi_sec123")
                .eventType("payment_intent.succeeded")
                .build();

        boolean processed = paymentService.processWebhook(webhook);

        assertThat(processed).isTrue();
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(testOrder.getPaymentMethod()).contains("UPI");
        verify(orderEventService, times(1)).publishOrderEvent(eq(1L), any(OrderResponseDTO.class));
    }
}
