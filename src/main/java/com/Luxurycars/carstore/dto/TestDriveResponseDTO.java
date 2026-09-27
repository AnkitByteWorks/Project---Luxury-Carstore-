package com.Luxurycars.carstore.dto;

import com.Luxurycars.carstore.entity.ExperienceType;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDriveResponseDTO {

    private Long id;
    private Long carId;
    private String carName;
    private String carBrand;
    private String customerName;
    private String phone;
    private String email;
    private LocalDate preferredDate;
    private String timeSlot;
    private ExperienceType experienceType;
    private String referenceCode;
    private TestDriveStatus status;
    private LocalDateTime createdAt;
}
