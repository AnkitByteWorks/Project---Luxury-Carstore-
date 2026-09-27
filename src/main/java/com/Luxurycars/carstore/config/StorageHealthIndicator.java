package com.Luxurycars.carstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class StorageHealthIndicator implements HealthIndicator {

    private final String uploadDir;

    public StorageHealthIndicator(@Value("${app.upload.dir:/app/uploads/cars}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public Health health() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.canWrite()) {
            return Health.down()
                    .withDetail("storagePath", uploadDir)
                    .withDetail("error", "Directory is not writable")
                    .build();
        }

        long usableSpaceMb = dir.getUsableSpace() / (1024 * 1024);
        long totalSpaceMb = dir.getTotalSpace() / (1024 * 1024);

        return Health.up()
                .withDetail("storagePath", uploadDir)
                .withDetail("usableSpaceMb", usableSpaceMb)
                .withDetail("totalSpaceMb", totalSpaceMb)
                .withDetail("status", "Healthy & Writable")
                .build();
    }
}
