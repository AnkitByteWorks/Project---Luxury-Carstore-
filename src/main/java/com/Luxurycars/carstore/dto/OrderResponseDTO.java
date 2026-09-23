package com.Luxurycars.carstore.dto;



import com.Luxurycars.carstore.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private Long id;

    // Car info (snapshot)
    private Long carId;
    private String carName;
    private String carImageUrl;      // "/api/cars/{carId}/image"

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;

    // Customer
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Delivery
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPincode;

    // Payment & status
    private String paymentMethod;
    private OrderStatus status;

    // Timestamps
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
}
