package com.Luxurycars.carstore.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB Document storing high-frequency IoT sensor telemetry
 * for hypercars during transport or track test drives.
 */
@Document(collection = "vehicle_telemetry_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTelemetryLog {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    private String carrierId;

    private Double speedKmH;

    private Double gForce;

    private Integer rpm;

    private Double cabinTempCelsius;

    private Double batteryTempCelsius;

    private Double gpsLatitude;

    private Double gpsLongitude;

    private String currentWaypoint;

    @Indexed
    private LocalDateTime recordedAt;
}
