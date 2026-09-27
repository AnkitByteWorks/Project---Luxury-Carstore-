package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentRequestDTO {

    @NotNull(message = "Order ID is required")
    @Schema(description = "ID of the order to pay for", example = "1")
    private Long orderId;

    @Schema(description = "Payment method chosen by customer", example = "CARD")
    @Builder.Default
    private String paymentMethod = "CARD";
}
