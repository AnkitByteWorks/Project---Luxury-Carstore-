package com.Luxurycars.carstore.kafka;

import com.Luxurycars.carstore.kafka.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class
CarstoreKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(CarstoreKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CarstoreKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate unavailable. Skipping event dispatch for Order #{}", event.getOrderId());
            return;
        }

        String key = String.valueOf(event.getOrderId());
        log.info("🚀 [KAFKA-PRODUCER] Dispatching OrderPlacedEvent to topic [{}] | Order #{} | Car: {}",
                KafkaTopicConfig.TOPIC_ORDERS, event.getOrderId(), event.getCarName());

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(KafkaTopicConfig.TOPIC_ORDERS, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ [KAFKA-PRODUCER] Event delivered to topic [{}] partition [{}] offset [{}]",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("❌ [KAFKA-PRODUCER] Failed to deliver OrderPlacedEvent for Order #{}: {}",
                            event.getOrderId(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("⚠️ [KAFKA-PRODUCER] Synchronous dispatch error: {}", e.getMessage());
        }
    }

    public void sendAuctionEvent(String auctionId, Object payload) {
        if (kafkaTemplate == null) return;
        try {
            kafkaTemplate.send(KafkaTopicConfig.TOPIC_AUCTIONS, auctionId, payload);
            log.info("⚡ [KAFKA-PRODUCER] Auction event dispatched for [{}]", auctionId);
        } catch (Exception e) {
            log.warn("Failed to dispatch auction event: {}", e.getMessage());
        }
    }

    public void sendTelemetryEvent(String vehicleId, Object payload) {
        if (kafkaTemplate == null) return;
        try {
            kafkaTemplate.send(KafkaTopicConfig.TOPIC_TELEMETRY, vehicleId, payload);
            log.info("📡 [KAFKA-PRODUCER] Telemetry event dispatched for [{}]", vehicleId);
        } catch (Exception e) {
            log.warn("Failed to dispatch telemetry event: {}", e.getMessage());
        }
    }
}
