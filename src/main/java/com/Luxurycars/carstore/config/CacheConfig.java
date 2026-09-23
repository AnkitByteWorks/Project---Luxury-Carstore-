package com.Luxurycars.carstore.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CARS_CACHE = "cars";
    public static final String CAR_CACHE = "car";
    public static final String CARS_STATS_CACHE = "carsStats";
    public static final String FEATURED_CACHE = "featured";
    public static final String LATEST_CACHE = "latest";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CARS_CACHE,
                CAR_CACHE,
                CARS_STATS_CACHE,
                FEATURED_CACHE,
                LATEST_CACHE
        );

        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                        // max 1000 entries per cache
                .expireAfterWrite(10, TimeUnit.MINUTES)   // auto-expire after 10 min
                .recordStats()                            // enable hit/miss stats
        );

        return manager;
    }
}