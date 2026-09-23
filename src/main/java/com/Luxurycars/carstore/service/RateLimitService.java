package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final RateLimitProperties props;

    private final Cache<String, Bucket> cache;

    @Autowired
    public RateLimitService(RateLimitProperties props) {
        this.props = props;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    // ─── Try to consume 1 token for this IP + category ───
    public boolean tryConsume(String ip, String category) {
        Bucket bucket = cache.get(ip + ":" + category, k -> createBucket(category));
        return bucket.tryConsume(1);
    }

    // ─── Build a bucket based on category ───
    private Bucket createBucket(String category) {
        RateLimitProperties.Limit limit = switch (category) {
            case "public" -> props.getPublicLimit();
            case "auth"   -> props.getAuth();
            case "admin"  -> props.getAdmin();
            case "upload" -> props.getUpload();
            default       -> props.getPublicLimit();
        };

        Refill refill = Refill.greedy(
                limit.getRefillPerMinute(),
                Duration.ofMinutes(1)
        );

        Bandwidth bandwidth = Bandwidth.classic(limit.getCapacity(), refill);

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    // ─── Seconds until next token (for Retry-After header) ───
    public long secondsUntilRefill(String ip, String category) {
        Bucket bucket = cache.getIfPresent(ip + ":" + category);
        if (bucket == null) return 0;

        EstimationProbe probe = bucket.estimateAbilityToConsume(1);
        if (probe.canBeConsumed()) {
            return 0;
        }
        // getNanosToWaitForRefill() returns nanoseconds
        return probe.getNanosToWaitForRefill() / 1_000_000_000L;
    }
}