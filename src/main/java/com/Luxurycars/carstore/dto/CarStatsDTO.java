package com.Luxurycars.carstore.dto;



import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarStatsDTO {

    private long totalCars;
    private long carsWithImages;
    private BigDecimal totalInventoryValue;   // sum of all prices
    private BigDecimal averagePrice;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private Map<String, Long> carsByBrand;    // brand → count
    private Map<String, Long> carsByLocation; // city → count
}
