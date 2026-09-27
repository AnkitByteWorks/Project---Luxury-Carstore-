package com.Luxurycars.carstore.dto;

import com.Luxurycars.carstore.entity.ExperienceType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDriveRequestDTO {

    @NotNull(message = "Car ID is required")
    private Long carId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Preferred date is required")
    private LocalDate preferredDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot;

    @NotNull(message = "Experience type is required (SHOWROOM or DOORSTEP)")
    private ExperienceType experienceType;
}
