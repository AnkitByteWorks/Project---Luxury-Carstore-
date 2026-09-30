package com.Luxurycars.carstore.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB Document representing customer behavioral analytics,
 * 3D component inspection events, test drive requests, and audit trials.
 */
@Document(collection = "customer_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAuditLog {

    @Id
    private String id;

    @Indexed
    private String customerIdentifier; // Email, phone, or session UUID

    private String customerName;

    @Indexed
    private String actionType; // e.g. "TEST_DRIVE_REQUEST", "3D_ENGINE_EXPLODED_VIEW", "DOOR_OPENED", "AUCTION_BID"

    private String carModel;

    private Long carId;

    private Map<String, Object> eventMetadata;

    private String ipAddress;

    @Indexed
    private LocalDateTime timestamp;
}
