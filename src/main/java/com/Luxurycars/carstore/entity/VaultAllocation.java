package com.Luxurycars.carstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vault_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultAllocation {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String builder;

    @Column(name = "chassis_number", nullable = false, length = 255)
    private String chassisNumber;

    @Column(name = "production_run", nullable = false, length = 255)
    private String productionRun;

    @Column(name = "engine_specs", nullable = false, length = 500)
    private String engineSpecs;

    @Column(nullable = false)
    private Integer horsepower;

    @Column(name = "top_speed_kmh", nullable = false)
    private Integer topSpeedKmH;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, length = 100)
    private String status;
}
