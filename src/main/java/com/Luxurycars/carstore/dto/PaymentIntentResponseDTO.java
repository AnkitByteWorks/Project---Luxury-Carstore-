package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponseDTO {

    @Schema(description = "Unique Payment Intent Identifier", example = "pi_lux_983bca91")
    private String paymentIntentId;

    @Schema(description = "Associated Order ID", example = "1")
    private Long orderId;

    @Schema(description = "Total payable amount", example = "400000000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    @Builder.Default
    private String currency = "INR";

    @Schema(description = "Client secret token for checkout UI")
    private String clientSecret;

    @Schema(description = "Payment Intent Status", example = "REQUIRES_PAYMENT")
    private String status;

    @Schema(description = "Simulation checkout redirect link")
    private String checkoutUrl;
}
