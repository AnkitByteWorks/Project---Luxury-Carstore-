package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventServiceTest {

    private final OrderEventService orderEventService = new OrderEventService();

    @Test
    @DisplayName("subscribe - should return SseEmitter")
    void subscribe_shouldReturnEmitter() {
        SseEmitter emitter = orderEventService.subscribe(101L);
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("publishOrderEvent - should broadcast without exceptions")
    void publishOrderEvent_shouldBroadcast() {
        SseEmitter emitter = orderEventService.subscribe(101L);

        OrderResponseDTO dto = OrderResponseDTO.builder()
                .id(101L)
                .carId(5L)
                .carName("Bugatti Chiron Super Sport")
                .unitPrice(new BigDecimal("400000000.00"))
                .quantity(1)
                .totalAmount(new BigDecimal("400000000.00"))
                .customerName("Ankit Singh")
                .customerEmail("ankit@luxurycars.com")
                .status(OrderStatus.PROCESSING)
                .orderedAt(LocalDateTime.now())
                .build();

        // Should broadcast cleanly
        orderEventService.publishOrderEvent(101L, dto);
    }
}
