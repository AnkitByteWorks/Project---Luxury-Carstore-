package com.Luxurycars.carstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_drives", indexes = {
        @Index(name = "idx_test_drives_ref_code", columnList = "reference_code"),
        @Index(name = "idx_test_drives_email", columnList = "email"),
        @Index(name = "idx_test_drives_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Car ID is required")
    @Column(name = "car_id", nullable = false)
    private Long carId;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, length = 100)
    private String email;

    @NotNull(message = "Preferred date is required")
    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @NotBlank(message = "Time slot is required")
    @Column(name = "time_slot", nullable = false, length = 50)
    private String timeSlot;

    @NotNull(message = "Experience type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "experience_type", nullable = false, length = 20)
    private ExperienceType experienceType;

    @NotBlank(message = "Reference code is required")
    @Column(name = "reference_code", nullable = false, unique = true, length = 50)
    private String referenceCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TestDriveStatus status = TestDriveStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TestDriveStatus.PENDING;
        }
    }
}
