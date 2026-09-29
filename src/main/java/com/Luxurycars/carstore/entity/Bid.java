package com.Luxurycars.carstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "bidder_name", nullable = false, length = 100)
    private String bidderName;

    @Column(name = "bidder_location", length = 100)
    private String bidderLocation;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bid_placed_at", nullable = false)
    private LocalDateTime bidPlacedAt;

    @PrePersist
    protected void onCreate() {
        if (bidPlacedAt == null) {
            bidPlacedAt = LocalDateTime.now();
        }
    }
}
