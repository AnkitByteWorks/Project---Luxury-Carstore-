package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookRequestDTO {

    @NotBlank(message = "Payment Intent ID is required")
    @Schema(description = "Payment Intent ID", example = "pi_lux_983bca91")
    private String paymentIntentId;

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "1")
    private Long orderId;

    @NotBlank(message = "Event type is required")
    @Schema(description = "Webhook Event Type", example = "payment_intent.succeeded")
    private String eventType;

    @Schema(description = "HMAC Webhook signature for verification")
    private String signature;
}
