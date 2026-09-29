package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponseDTO {

    @Schema(description = "Bid ID", example = "101")
    private Long id;

    @Schema(description = "Associated Auction Lot ID", example = "1")
    private Long lotId;

    @Schema(description = "Bidder Display Name", example = "Lord Sterling")
    private String bidderName;

    @Schema(description = "Bidder Location", example = "Monaco")
    private String bidderLocation;

    @Schema(description = "Winning Bid Amount", example = "290000000.00")
    private BigDecimal amount;

    @Schema(description = "Bid timestamp")
    private LocalDateTime bidPlacedAt;

    @Schema(description = "New current high bid on the lot", example = "290000000.00")
    private BigDecimal currentBid;

    @Schema(description = "Whether the reserve price has been met", example = "true")
    private Boolean reserveMet;

    @Schema(description = "Minimum next required bid", example = "290500001.00")
    private BigDecimal minNextBid;
}
