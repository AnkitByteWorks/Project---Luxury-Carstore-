package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.mongodb.document.CustomerAuditLog;
import com.Luxurycars.carstore.mongodb.document.VehicleTelemetryLog;
import com.Luxurycars.carstore.mongodb.repository.CustomerAuditLogRepository;
import com.Luxurycars.carstore.mongodb.repository.VehicleTelemetryLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller showcasing Polyglot Persistence with MongoDB.
 * Records and retrieves unstructured clickstream analytics,
 * 3D component inspection telemetry, and audit trails.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Telemetry (MongoDB)", description = "Polyglot NoSQL customer interactions, 3D model inspection events, and live IoT telemetry")
public class AuditTelemetryController {

    private final CustomerAuditLogRepository customerAuditLogRepository;
    private final VehicleTelemetryLogRepository vehicleTelemetryLogRepository;

    @Autowired
    public AuditTelemetryController(
            @Autowired(required = false) CustomerAuditLogRepository customerAuditLogRepository,
            @Autowired(required = false) VehicleTelemetryLogRepository vehicleTelemetryLogRepository) {
        this.customerAuditLogRepository = customerAuditLogRepository;
        this.vehicleTelemetryLogRepository = vehicleTelemetryLogRepository;
    }

    @Operation(summary = "Record customer 3D interaction / clickstream event in MongoDB",
            description = "Stores rich, flexible JSON event data (e.g. 3D door open, engine exploded view, interior 360 inspect)")
    @PostMapping("/interaction")
    public ResponseEntity<Map<String, Object>> recordInteraction(@RequestBody Map<String, Object> payload) {
        if (customerAuditLogRepository != null) {
            String customerId = (String) payload.getOrDefault("customerId", "ANONYMOUS_COLLECTOR");
            String customerName = (String) payload.getOrDefault("customerName", "VIP Visitor");
            String actionType = (String) payload.getOrDefault("actionType", "3D_INSPECTION");
            String carModel = (String) payload.getOrDefault("carModel", "Pagani Huayra R");

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) payload.getOrDefault("metadata", Map.of());

            CustomerAuditLog logEntry = CustomerAuditLog.builder()
                    .customerIdentifier(customerId)
                    .customerName(customerName)
                    .actionType(actionType)
                    .carModel(carModel)
                    .eventMetadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();

            CustomerAuditLog saved = customerAuditLogRepository.save(logEntry);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "RECORDED",
                    "mongoId", saved.getId(),
                    "collection", "customer_audit_logs",
                    "timestamp", saved.getTimestamp()
            ));
        }

        return ResponseEntity.ok(Map.of("status", "MONGODB_DISABLED"));
    }

    @Operation(summary = "Get recent customer audit logs from MongoDB")
    @GetMapping("/customer-audit")
    public ResponseEntity<List<CustomerAuditLog>> getRecentAuditLogs() {
        if (customerAuditLogRepository != null) {
            return ResponseEntity.ok(customerAuditLogRepository.findTop20ByOrderByTimestampDesc());
        }
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Get live vehicle telemetry stream from MongoDB")
    @GetMapping("/telemetry/{orderId}")
    public ResponseEntity<List<VehicleTelemetryLog>> getVehicleTelemetry(@PathVariable Long orderId) {
        if (vehicleTelemetryLogRepository != null) {
            return ResponseEntity.ok(vehicleTelemetryLogRepository.findByOrderIdOrderByRecordedAtDesc(orderId));
        }
        return ResponseEntity.ok(List.of());
    }
}
