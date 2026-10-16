package com.Luxurycars.carstore.kafka;

import com.Luxurycars.carstore.kafka.dto.KafkaLogEntry;
import com.Luxurycars.carstore.kafka.dto.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class CarstoreKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CarstoreKafkaConsumer.class);
    private static final int MAX_HISTORY = 50;

    private final Deque<KafkaLogEntry> recentEvents = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalConsumedCount = new AtomicLong(0);

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_ORDERS, groupId = "carstore-group")
    public void consumeOrderEvent(ConsumerRecord<String, OrderPlacedEvent> record) {
        OrderPlacedEvent event = record.value();
        long count = totalConsumedCount.incrementAndGet();

        log.info("📥 [KAFKA-CONSUMER] Received OrderPlacedEvent #{} | Key: {} | Partition: {} | Offset: {}",
                count, record.key(), record.partition(), record.offset());
        log.info("🚗 [CARSTORE-EVENT] Order #{} for {} ({}) by {}",
                event.getOrderId(), event.getCarName(), event.getBrand(), event.getCustomerName());

        List<String> simulatedActions = new ArrayList<>();

        // 1. Bespoke Atelier Microservice
        if (event.getMonogramText() != null && !event.getMonogramText().trim().isEmpty()) {
            String monogramMsg = String.format("🔑 [ATELIER SERVICE] Laser monogramming queued: '%s' in %s finish",
                    event.getMonogramText(),
                    event.getMonogramColor() != null ? event.getMonogramColor() : "24K Gold");
            simulatedActions.add(monogramMsg);
            log.info(monogramMsg);
        }

        // 2. White-Glove Transporter Microservice
        String logisticsMsg = String.format("🚚 [LOGISTICS SERVICE] Enclosed climate-controlled carrier scheduled for destination: %s",
                event.getDeliveryCity() != null ? event.getDeliveryCity() : "Confidential Client Vault");
        simulatedActions.add(logisticsMsg);
        log.info(logisticsMsg);

        // 3. VIP Concierge Service
        String conciergeMsg = String.format("🎩 [CONCIERGE SERVICE] Dedicated Luxury Relationship Manager assigned to VIP %s (%s)",
                event.getCustomerName(), event.getCustomerEmail());
        simulatedActions.add(conciergeMsg);
        log.info(conciergeMsg);

        // Store in memory ring buffer for live inspection
        KafkaLogEntry logEntry = KafkaLogEntry.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .eventType(event.getEventType() != null ? event.getEventType() : "ORDER_PLACED")
                .consumedAt(LocalDateTime.now())
                .payload(event)
                .simulatedActions(simulatedActions)
                .build();

        recentEvents.addFirst(logEntry);
        while (recentEvents.size() > MAX_HISTORY) {
            recentEvents.pollLast();
        }
    }

    public List<KafkaLogEntry> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    public long getTotalConsumedCount() {
        return totalConsumedCount.get();
    }
}
