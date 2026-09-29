package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.PaymentIntentRequestDTO;
import com.Luxurycars.carstore.dto.PaymentIntentResponseDTO;
import com.Luxurycars.carstore.dto.PaymentWebhookRequestDTO;
import com.Luxurycars.carstore.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Sandbox payment intent creation and automated webhook verification")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a sandbox payment intent for an order")
    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponseDTO> createIntent(
            @Valid @RequestBody PaymentIntentRequestDTO dto) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(dto));
    }

    @Operation(summary = "Create payment intent for an order (UPI QR Code / Card)")
    @PostMapping("/intent/{orderId}")
    public ResponseEntity<PaymentIntentResponseDTO> createIntentForOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false, defaultValue = "UPI") String paymentMethod,
            @RequestBody(required = false) PaymentIntentRequestDTO body) {
        String method = (body != null && body.getPaymentMethod() != null && !body.getPaymentMethod().isBlank())
                ? body.getPaymentMethod()
                : paymentMethod;
        return ResponseEntity.ok(paymentService.createPaymentIntent(orderId, method));
    }

    @Operation(summary = "Payment gateway webhook callback (Public)")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @Valid @RequestBody PaymentWebhookRequestDTO dto) {
        boolean processed = paymentService.processWebhook(dto);
        return ResponseEntity.ok(Map.of(
                "received", true,
                "status", processed ? "CONFIRMED" : "IGNORED_OR_FAILED"
        ));
    }
}
