package com.Luxurycars.carstore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Limit publicLimit = new Limit();
    private Limit auth = new Limit();
    private Limit admin = new Limit();
    private Limit upload = new Limit();

    @Getter
    @Setter
    public static class Limit {
        private long capacity = 60;
        private long refillPerMinute = 60;
    }
}
