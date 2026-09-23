package com.Luxurycars.carstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity                              // ← tells JPA: "this class = a DB table"
@Table(name = "cars")                // ← actual table name in MySQL = "cars"
@Getter                              // ← Lombok auto-generates getters
@Setter                              // ← Lombok auto-generates setters
@NoArgsConstructor                   // ← Lombok: empty constructor (JPA needs it)
@AllArgsConstructor                  // ← Lombok: constructor with all fields
@Builder                             // ← Lombok: builder pattern (nice for creating objects)
public class Car {

    @Id                                              // ← primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← AUTO_INCREMENT in MySQL
    private Long id;

    @NotBlank(message = "Car name is required")       // ← validation: can't be blank
    @Column(nullable = false, length = 100)
    private String name;                              // e.g., "Porsche 911 Turbo S"

    @NotBlank(message = "Brand is required")
    @Column(nullable = false, length = 50)
    private String brand;                             // e.g., "Porsche"

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;                         // e.g., 25000000.00 (INR)
    // ↑ BigDecimal is used for money (no floating-point errors)

    @Column(length = 2000)
    private String description;                       // short description

    @Column(name = "color_options", length = 255)
    private String colorOptions;                      // e.g., "Red,Black,Silver"

    @Column(name = "showroom_location", length = 100)
    private String showroomLocation;                  // e.g., "Mumbai"

    @Column(name = "delivery_days")
    private Integer deliveryDays;                     // e.g., 30

    @Column(name = "payment_options", length = 255)
    private String paymentOptions;                    // e.g., "Cash,EMI,Card"

    // ─── IMAGE FIELDS ───
    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_name", length = 255)
    private String imageName;                         // e.g., "porsche-911.jpg"

    @Column(name = "image_type", length = 50)
    private String imageType;                         // e.g., "image/jpeg"

    // ─── AUDIT FIELDS ───
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;                  // when car was added

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;                  // last update

    // ─── AUTO TIMESTAMPS ───
    @PrePersist                                       // ← runs BEFORE insert
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate                                        // ← runs BEFORE update
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
