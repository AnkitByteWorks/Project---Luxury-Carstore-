package com.Luxurycars.carstore.kafka;

import com.Luxurycars.carstore.kafka.dto.KafkaLogEntry;
import com.Luxurycars.carstore.kafka.dto.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/kafka")
@CrossOrigin(origins = "*")
public class KafkaMonitorController {

    private final CarstoreKafkaProducer producer;
    private final CarstoreKafkaConsumer consumer;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:carstore-group}")
    private String consumerGroup;

    @Autowired
    public KafkaMonitorController(@Autowired(required = false) CarstoreKafkaProducer producer,
                                  @Autowired(required = false) CarstoreKafkaConsumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getKafkaStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean isAvailable = producer != null && consumer != null;

        status.put("status", isAvailable ? "ONLINE" : "STANDBY");
        status.put("broker", bootstrapServers);
        status.put("consumerGroup", consumerGroup);
        status.put("topics", List.of(
                KafkaTopicConfig.TOPIC_ORDERS,
                KafkaTopicConfig.TOPIC_AUCTIONS,
                KafkaTopicConfig.TOPIC_TELEMETRY
        ));
        status.put("totalEventsConsumed", consumer != null ? consumer.getTotalConsumedCount() : 0);
        status.put("recentEventsInMemory", consumer != null ? consumer.getRecentEvents().size() : 0);
        status.put("architecture", "Apache Kafka KRaft Mode (Event-Driven Microservices Decoupling)");

        return ResponseEntity.ok(status);
    }

    @GetMapping("/events")
    public ResponseEntity<List<KafkaLogEntry>> getRecentEvents() {
        if (consumer == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(consumer.getRecentEvents());
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> publishTestEvent(
            @RequestParam(defaultValue = "Bugatti Chiron Pur Sport") String carName,
            @RequestParam(defaultValue = "Lord Sterling") String customerName,
            @RequestParam(defaultValue = "Monaco") String city,
            @RequestParam(defaultValue = "ROYAL-007") String monogram) {

        if (producer == null) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Kafka Producer is currently unavailable"
            ));
        }

        long fakeOrderId = 90000L + new Random().nextInt(9999);
        OrderPlacedEvent testEvent = OrderPlacedEvent.builder()
                .orderId(fakeOrderId)
                .carId(1L)
                .carName(carName)
                .brand("Bugatti")
                .customerName(customerName)
                .customerEmail("vip.collector@sanctuary.luxury")
                .customerPhone("+377 98 06 20 00")
                .deliveryCity(city)
                .totalAmount(new BigDecimal("3600000.00"))
                .monogramText(monogram)
                .monogramColor("24K Gold Leaf")
                .paymentMethod("CRYPTO_ESCROW")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .eventType("TEST_ORDER_SIMULATION")
                .build();

        producer.sendOrderPlacedEvent(testEvent);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Simulated Order event published to topic [" + KafkaTopicConfig.TOPIC_ORDERS + "]");
        resp.put("simulatedOrderId", fakeOrderId);
        resp.put("payload", testEvent);
        resp.put("instructions", "Check /api/kafka/events or your terminal/Docker logs to see the consumer asynchronously pick it up!");

        return ResponseEntity.ok(resp);
    }
}
