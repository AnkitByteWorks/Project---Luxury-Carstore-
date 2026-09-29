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
public class AuctionLotResponseDTO {

    @Schema(description = "Auction Lot ID", example = "1")
    private Long id;

    @Schema(description = "Hypercar Title / Model", example = "2024 Pagani Huayra R")
    private String title;

    @Schema(description = "Detailed provenance & vehicle description")
    private String description;

    @Schema(description = "High-resolution hero vehicle photo URL")
    private String imageUrl;

    @Schema(description = "Starting bid price", example = "250000000.00")
    private BigDecimal startingBid;

    @Schema(description = "Current highest bid", example = "285000000.00")
    private BigDecimal currentBid;

    @Schema(description = "Minimum bid increment required", example = "500000.00")
    private BigDecimal minIncrement;

    @Schema(description = "Whether the seller's secret reserve price has been met", example = "true")
    private Boolean reserveMet;

    @Schema(description = "Whether this auction lot is currently live and accepting bids", example = "true")
    private Boolean active;

    @Schema(description = "Lot closing timestamp")
    private LocalDateTime lotEndsAt;

    @Schema(description = "Time remaining in seconds before lot closes", example = "7200")
    private Long countdownSeconds;

    @Schema(description = "Human-readable countdown string", example = "02h 00m 00s")
    private String countdown;

    @Schema(description = "Total number of bids placed on this lot", example = "14")
    private Integer totalBids;

    @Schema(description = "Strict minimum next bid required to overtake leader", example = "285500001.00")
    private BigDecimal minNextBid;
}
