package com.Luxurycars.carstore.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaLogEntry {
    private String eventId;
    private String topic;
    private int partition;
    private long offset;
    private String eventType;
    private LocalDateTime consumedAt;
    private Object payload;
    private List<String> simulatedActions;
}
