package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.CacheConfig;
import com.Luxurycars.carstore.config.JwtAuthFilter;
import com.Luxurycars.carstore.config.RateLimitFilter;
import com.Luxurycars.carstore.config.SecurityConfig;
import com.Luxurycars.carstore.dto.PaymentIntentResponseDTO;
import com.Luxurycars.carstore.dto.PaymentWebhookRequestDTO;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
import com.Luxurycars.carstore.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import({SecurityConfig.class, JwtAuthFilter.class, CacheConfig.class})
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/payments/intent/{orderId} - should create intent with UPI QR code string")
    void createIntentForOrder_shouldSupportUpi() throws Exception {
        String expectedUpi = "upi://pay?pa=carstore.bespoke@icici&pn=CarstoreVIP&am=400000000.00&tr=1&cu=INR";
        PaymentIntentResponseDTO responseDTO = PaymentIntentResponseDTO.builder()
                .paymentIntentId("upi_123456")
                .orderId(1L)
                .amount(new BigDecimal("400000000.00"))
                .currency("INR")
                .status("REQUIRES_PAYMENT")
                .paymentMethod("UPI")
                .upiString(expectedUpi)
                .build();

        when(paymentService.createPaymentIntent(eq(1L), eq("UPI"))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/payments/intent/1")
                        .param("paymentMethod", "UPI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.upiString").value(expectedUpi));
    }

    @Test
    @DisplayName("POST /api/payments/webhook - should accept payment_intent.succeeded webhook")
    void handleWebhook_shouldProcessSucceededEvent() throws Exception {
        PaymentWebhookRequestDTO webhookDTO = PaymentWebhookRequestDTO.builder()
                .orderId(1L)
                .paymentIntentId("upi_98765")
                .eventType("payment_intent.succeeded")
                .build();

        when(paymentService.processWebhook(any(PaymentWebhookRequestDTO.class))).thenReturn(true);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
