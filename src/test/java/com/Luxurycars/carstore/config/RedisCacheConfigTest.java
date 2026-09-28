package com.Luxurycars.carstore.config;

import com.Luxurycars.carstore.dto.CarResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    @DisplayName("cacheManager - should configure RedisCacheManager with defined cache names and 10min TTL")
    void cacheManager_shouldConfigureRedisCacheManager() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        CacheManager manager = cacheConfig.cacheManager(connectionFactory);

        assertThat(manager).isInstanceOf(RedisCacheManager.class);
        RedisCacheManager redisManager = (RedisCacheManager) manager;

        assertThat(redisManager.getCacheNames()).contains(
                CacheConfig.CARS_CACHE,
                CacheConfig.CAR_CACHE,
                CacheConfig.FEATURED_CACHE,
                CacheConfig.CARS_STATS_CACHE
        );
    }

    @Test
    @DisplayName("JavaTimeModule & Jackson serialization - handles LocalDateTime in DTOs cleanly")
    void jacksonSerialization_shouldHandleJavaTimeCleanly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        CarResponseDTO dto = CarResponseDTO.builder()
                .id(1L)
                .name("Rolls-Royce Ghost")
                .brand("Rolls-Royce")
                .price(new BigDecimal("50000000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("Rolls-Royce Ghost");
        assertThat(json).doesNotContain("\"createdAt\":["); // Confirms ISO string, not numeric array

        CarResponseDTO deserialized = mapper.readValue(json, CarResponseDTO.class);
        assertThat(deserialized.getName()).isEqualTo(dto.getName());
        assertThat(deserialized.getCreatedAt()).isEqualTo(dto.getCreatedAt());
    }
}
