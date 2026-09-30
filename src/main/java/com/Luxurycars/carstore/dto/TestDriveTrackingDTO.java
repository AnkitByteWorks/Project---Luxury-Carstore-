package com.Luxurycars.carstore.dto;

import com.Luxurycars.carstore.entity.TestDriveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDriveTrackingDTO {
    private String referenceCode;
    private Long carId;
    private String carName;
    private String carBrand;
    private String customerName;
    private LocalDate preferredDate;
    private String timeSlot;
    private String experienceType;
    private TestDriveStatus currentStatus;
    private int currentStep; // 1 to 5
    private List<TrackingStep> steps;
    private ConciergeInfo concierge;
    private LogisticsInfo logistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingStep {
        private int stepNumber;
        private String title;
        private String description;
        private boolean completed;
        private boolean active;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConciergeInfo {
        private String name;
        private String title;
        private String phone;
        private String badge;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogisticsInfo {
        private String carrierId;
        private String transporterType;
        private String driverName;
        private String currentCheckpoint;
        private String climateControlTemp;
        private String estimatedArrival;
    }
}
