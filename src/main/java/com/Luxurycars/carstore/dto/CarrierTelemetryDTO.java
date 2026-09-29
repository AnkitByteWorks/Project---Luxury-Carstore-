package com.Luxurycars.carstore.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierTelemetryDTO {
    private Long orderId;
    private String carrierId;
    private String carrierName;
    private String driverName;
    private String currentWaypoint;
    private Integer transitSpeedKmH;
    private Double trailerTempCelsius;
    private Integer progressPercent;
    private LocalDateTime estimatedArrival;
    private String destinationCity;
    private String transportSecurityStatus;
}
