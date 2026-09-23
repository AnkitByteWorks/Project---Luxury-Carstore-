package com.Luxurycars.carstore.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayRunner.class);
    private final Flyway flyway;

    @Autowired
    public FlywayRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) {
        log.info("🔥🔥🔥 FLYWAY RUNNER — Starting migrations 🔥🔥🔥");
        var result = flyway.migrate();
        log.info("🔥🔥🔥 FLYWAY DONE — Executed: "
                + result.migrationsExecuted + " migrations 🔥🔥🔥");
    }
}