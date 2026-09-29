package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidRequestDTO {

    @Schema(description = "Name or VIP Alias of the collector placing the bid", example = "Lord Sterling")
    private String bidderName;

    @Schema(description = "City / Region of the bidder", example = "Monaco")
    private String bidderLocation;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than zero")
    @Schema(description = "Submitted bid amount in INR", example = "290000000.00")
    private BigDecimal amount;
}
