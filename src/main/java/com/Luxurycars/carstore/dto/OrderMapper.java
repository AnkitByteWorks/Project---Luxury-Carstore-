package com.Luxurycars.carstore.dto;

import com.Luxurycars.carstore.entity.Order;

public class OrderMapper {

    public static OrderResponseDTO toResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .carId(order.getCarId())
                .carName(order.getCarName())
                .carImageUrl("/api/cars/" + order.getCarId() + "/image")
                .unitPrice(order.getUnitPrice())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryCity(order.getDeliveryCity())
                .deliveryPincode(order.getDeliveryPincode())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .orderedAt(order.getOrderedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}