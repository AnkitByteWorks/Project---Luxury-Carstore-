package com.Luxurycars.carstore.kafka;

import com.Luxurycars.carstore.kafka.dto.KafkaLogEntry;
import com.Luxurycars.carstore.kafka.dto.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarstoreKafkaTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CarstoreKafkaProducer producer;
    private CarstoreKafkaConsumer consumer;
    private KafkaMonitorController controller;

    @BeforeEach
    void setUp() {
        producer = new CarstoreKafkaProducer(kafkaTemplate);
        consumer = new CarstoreKafkaConsumer();
        controller = new KafkaMonitorController(producer, consumer);
    }

    @Test
    @DisplayName("Producer dispatches OrderPlacedEvent to Kafka topic")
    void testProducerDispatch() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(101L)
                .carId(1L)
                .carName("Rolls-Royce Phantom")
                .brand("Rolls-Royce")
                .customerName("Lord Mountbatten")
                .deliveryCity("London")
                .totalAmount(new BigDecimal("450000.00"))
                .monogramText("ROYAL-01")
                .eventType("ORDER_PLACED")
                .build();

        producer.sendOrderPlacedEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(KafkaTopicConfig.TOPIC_ORDERS), eq("101"), eq(event));
    }

    @Test
    @DisplayName("Consumer processes OrderPlacedEvent and triggers microservices simulation")
    void testConsumerProcessing() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(202L)
                .carName("Ferrari Daytona SP3")
                .brand("Ferrari")
                .customerName("Charles Leclerc")
                .customerEmail("leclerc@scuderia.it")
                .deliveryCity("Monaco")
                .monogramText("CL-16")
                .monogramColor("Rosso Corsa Red")
                .eventType("ORDER_PLACED")
                .build();

        ConsumerRecord<String, OrderPlacedEvent> record =
                new ConsumerRecord<>(KafkaTopicConfig.TOPIC_ORDERS, 0, 15L, "202", event);

        consumer.consumeOrderEvent(record);

        assertThat(consumer.getTotalConsumedCount()).isEqualTo(1);
        List<KafkaLogEntry> events = consumer.getRecentEvents();
        assertThat(events).hasSize(1);

        KafkaLogEntry entry = events.get(0);
        assertThat(entry.getTopic()).isEqualTo(KafkaTopicConfig.TOPIC_ORDERS);
        assertThat(entry.getOffset()).isEqualTo(15L);
        assertThat(entry.getSimulatedActions()).anyMatch(act -> act.contains("CL-16"));
        assertThat(entry.getSimulatedActions()).anyMatch(act -> act.contains("Monaco"));
        assertThat(entry.getSimulatedActions()).anyMatch(act -> act.contains("Charles Leclerc"));
    }

    @Test
    @DisplayName("Monitor Controller returns healthy status and events")
    void testControllerStatus() {
        ResponseEntity<Map<String, Object>> response = controller.getKafkaStatus();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ONLINE");
        assertThat(response.getBody().get("architecture").toString()).contains("Apache Kafka KRaft Mode");
    }
}
