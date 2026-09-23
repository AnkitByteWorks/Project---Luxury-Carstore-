package com.Luxurycars.carstore.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatsDTO {
    private long totalOrders;
    private BigDecimal totalRevenue;              // sum of all CONFIRMED + DELIVERED orders
    private BigDecimal pendingRevenue;            // sum of PENDING orders
    private Map<String, Long> ordersByStatus;     // status → count
}
