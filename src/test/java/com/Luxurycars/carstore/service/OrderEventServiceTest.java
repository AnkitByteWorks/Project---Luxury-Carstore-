package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderEventServiceTest {

    private final OrderEventService orderEventService = new OrderEventService();

    @Test
    @DisplayName("subscribe - should return SseEmitter")
    void subscribe_shouldReturnEmitter() {
        SseEmitter emitter = orderEventService.subscribe(101L);
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("publishOrderEvent - should broadcast without exceptions (fallback mode)")
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

    @Test
    @DisplayName("publishOrderEvent - with Redis should publish JSON to order-events channel")
    void publishOrderEvent_withRedis_shouldPublishToChannel() {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OrderEventService serviceWithRedis = new OrderEventService(stringRedisTemplate, objectMapper);

        OrderResponseDTO dto = OrderResponseDTO.builder()
                .id(102L)
                .carId(1L)
                .carName("Porsche 911")
                .status(OrderStatus.CONFIRMED)
                .build();

        serviceWithRedis.publishOrderEvent(102L, dto);

        verify(stringRedisTemplate, times(1)).convertAndSend(eq("order-events"), contains("102"));
    }

    @Test
    @DisplayName("onMessage - should receive Redis message and broadcast to local emitters")
    void onMessage_shouldReceiveRedisMessageAndBroadcast() throws Exception {
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OrderEventService service = new OrderEventService(stringRedisTemplate, objectMapper);

        SseEmitter emitter = service.subscribe(200L);
        assertThat(emitter).isNotNull();

        OrderResponseDTO dto = OrderResponseDTO.builder()
                .id(200L)
                .carId(2L)
                .carName("Ferrari SF90")
                .status(OrderStatus.SHIPPED)
                .build();

        byte[] body = objectMapper.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8);
        service.onMessage(new DefaultMessage("order-events".getBytes(StandardCharsets.UTF_8), body), null);

        // Verification that message handled without error
        assertThat(service.getOrderEmitters()).containsKey(200L);
    }

    @Test
    @DisplayName("sendHeartbeat - sends heartbeat ping to active connections")
    void sendHeartbeat_shouldSendPingToEmitters() {
        SseEmitter emitter = orderEventService.subscribe(300L);
        assertThat(emitter).isNotNull();

        // Should execute ping without failure
        orderEventService.sendHeartbeat();
        assertThat(orderEventService.getOrderEmitters()).containsKey(300L);
    }
}
