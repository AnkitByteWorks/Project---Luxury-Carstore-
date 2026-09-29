package com.Luxurycars.carstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "starting_bid", nullable = false, precision = 15, scale = 2)
    private BigDecimal startingBid;

    @Column(name = "current_bid", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBid;

    @Column(name = "reserve_price", precision = 15, scale = 2)
    private BigDecimal reservePrice;

    @Column(name = "min_increment", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minIncrement = new BigDecimal("500000.00");

    @Column(name = "reserve_met", nullable = false)
    @Builder.Default
    private Boolean reserveMet = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "lot_ends_at", nullable = false)
    private LocalDateTime lotEndsAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentBid == null) {
            currentBid = startingBid;
        }
        if (minIncrement == null) {
            minIncrement = new BigDecimal("500000.00");
        }
        if (reserveMet == null) {
            reserveMet = false;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
