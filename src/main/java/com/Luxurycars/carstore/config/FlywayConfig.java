package com.Luxurycars.carstore.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations.split(","))
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();

        try {
            log.info("🔧 Running Flyway repair to clean any failed schema history...");
            flyway.repair();
        } catch (Exception e) {
            log.warn("Flyway repair completed with notice: {}", e.getMessage());
        }

        try {
            log.info("🚀 Running Flyway migrate...");
            flyway.migrate();
            log.info("✅ Flyway migrations completed successfully.");
        } catch (Exception e) {
            log.error("❌ Flyway migration failed: {}", e.getMessage(), e);
            throw e;
        }

        return flyway;
    }

    @Bean
    public static org.springframework.beans.factory.config.BeanFactoryPostProcessor dependsOnFlywayPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")
                    && beanFactory.containsBeanDefinition("flyway")) {
                beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("flyway");
            }
        };
    }
}