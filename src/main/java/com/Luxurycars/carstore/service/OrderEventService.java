package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderEventService {

    private static final Logger log = LoggerFactory.getLogger(OrderEventService.class);

    // Map: orderId -> list of active SseEmitters
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> orderEmitters = new ConcurrentHashMap<>();

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
        List<SseEmitter> emitters = orderEmitters.get(orderId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE listeners for order {}", orderId);
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
}
