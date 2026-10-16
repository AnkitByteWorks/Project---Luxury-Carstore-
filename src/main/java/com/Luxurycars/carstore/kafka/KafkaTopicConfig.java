package com.Luxurycars.carstore.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    public static final String TOPIC_ORDERS = "carstore.orders";
    public static final String TOPIC_AUCTIONS = "carstore.auctions";
    public static final String TOPIC_TELEMETRY = "carstore.telemetry";

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(TOPIC_ORDERS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auctionsTopic() {
        return TopicBuilder.name(TOPIC_AUCTIONS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic telemetryTopic() {
        return TopicBuilder.name(TOPIC_TELEMETRY)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
