package com.Luxurycars.carstore.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponseDTO {

    // ─── BASIC FIELDS ───
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private String description;
    private String colorOptions;
    private String showroomLocation;
    private Integer deliveryDays;
    private String paymentOptions;

    // ─── IMAGE: only the URL, NOT the blob ───
    private String imageUrl;         // e.g., "/api/cars/1/image"
    private String imageName;        // e.g., "porsche-911.jpg"
    private boolean hasImage;
    private String imagePath;   // internal path (optional, could hide)// true / false — quick check for frontend

    // ─── AUDIT FIELDS ───
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
