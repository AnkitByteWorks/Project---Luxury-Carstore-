package com.Luxurycars.carstore.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long carId;
    private String carName;
    private String brand;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String deliveryCity;
    private BigDecimal totalAmount;
    private String monogramText;
    private String monogramColor;
    private String paymentMethod;
    private String timestamp;
    private String eventType;
}
