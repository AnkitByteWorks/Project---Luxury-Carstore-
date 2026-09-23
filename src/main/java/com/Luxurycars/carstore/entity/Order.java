package com.Luxurycars.carstore.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")            // ← avoid reserved word "order"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── WHICH CAR ───
    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "car_name", nullable = false, length = 100)
    private String carName;                 // snapshot (car name might change later)

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;           // snapshot price at order time

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;         // unitPrice × quantity

    // ─── CUSTOMER INFO ───
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 100)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    // ─── DELIVERY ───
    @Column(name = "delivery_address", length = 500, nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_pincode", length = 10)
    private String deliveryPincode;

    // ─── PAYMENT & STATUS ───
    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;           // "Cash", "EMI", "Bank Transfer", etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    // ─── TIMESTAMPS ───
    @Column(name = "ordered_at", updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        orderedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

