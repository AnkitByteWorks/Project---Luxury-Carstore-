package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderEventService implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventService.class);
    public static final String ORDER_EVENTS_CHANNEL = "order-events";

    // Map: orderId -> list of active SseEmitters
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> orderEmitters = new ConcurrentHashMap<>();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventService() {
        this(null, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Autowired
    public OrderEventService(@Autowired(required = false) StringRedisTemplate stringRedisTemplate,
                             @Autowired(required = false) ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper != null
                ? objectMapper
                : new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public SseEmitter subscribe(Long orderId) {
        log.info("Client subscribed to SSE events for order ID: {}", orderId);

        // 30 minutes timeout
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        orderEmitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(e -> removeEmitter(orderId, emitter));

        // Initial handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of(
                            "orderId", orderId,
                            "message", "Connected to live status stream for order #" + orderId,
                            "timestamp", LocalDateTime.now().toString()
                    )));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE handshake for order {}: {}", orderId, e.getMessage());
            removeEmitter(orderId, emitter);
        }

        return emitter;
    }

    public void publishOrderEvent(Long orderId, OrderResponseDTO order) {
        if (order != null && order.getId() == null && orderId != null) {
            order.setId(orderId);
        }

        if (stringRedisTemplate != null) {
            try {
                String payload = objectMapper.writeValueAsString(order);
                log.info("Publishing order event to Redis channel '{}' for order ID: {}", ORDER_EVENTS_CHANNEL, orderId);
                stringRedisTemplate.convertAndSend(ORDER_EVENTS_CHANNEL, payload);
                return;
            } catch (Exception e) {
                log.error("Failed to publish order event to Redis channel '{}' for order {}: {}",
                        ORDER_EVENTS_CHANNEL, orderId, e.getMessage(), e);
                // Fallback to local broadcast if Redis publish fails
            }
        }

        broadcastLocal(orderId, order);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("Received order event from Redis pub/sub channel '{}': {}", ORDER_EVENTS_CHANNEL, payload);
            OrderResponseDTO order = objectMapper.readValue(payload, OrderResponseDTO.class);
            if (order != null && order.getId() != null) {
                broadcastLocal(order.getId(), order);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis pub/sub order event message: {}", e.getMessage(), e);
        }
    }

    public void broadcastLocal(Long orderId, OrderResponseDTO order) {
        List<SseEmitter> emitters = orderEmitters.get(orderId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE listeners on this instance for order {}", orderId);
            return;
        }

        log.info("Broadcasting order status update event for order ID: {} to {} listener(s)", orderId, emitters.size());
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ORDER_STATUS_UPDATE")
                        .data(order));
            } catch (Exception e) {
                log.warn("Error sending SSE to client for order {}: {}", orderId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
        if (emitters.isEmpty()) {
            orderEmitters.remove(orderId);
        }
    }

    // ─── 25-SECOND HEARTBEAT PING EVENT ───
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (orderEmitters.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : orderEmitters.entrySet()) {
            Long orderId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("HEARTBEAT")
                            .data("ping"));
                } catch (Exception e) {
                    log.debug("Heartbeat failed for SSE emitter on order {}: {}", orderId, e.getMessage());
                    deadEmitters.add(emitter);
                }
            }

            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                orderEmitters.remove(orderId);
            }
        }
    }

    private void removeEmitter(Long orderId, SseEmitter emitter) {
        List<SseEmitter> emitters = orderEmitters.get(orderId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                orderEmitters.remove(orderId);
            }
        }
        log.debug("Removed SSE emitter for order {}", orderId);
    }

    public Map<Long, CopyOnWriteArrayList<SseEmitter>> getOrderEmitters() {
        return orderEmitters;
    }
}
