package com.Luxurycars.carstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car3dSpecDTO {

    @Schema(description = "Vehicle ID")
    private Long carId;

    @Schema(description = "Vehicle Name")
    private String carName;

    @Schema(description = "Vehicle Brand")
    private String brand;

    @Schema(description = "3D model download/stream endpoint", example = "/api/cars/1/3d-model")
    private String model3dUrl;

    @Schema(description = "Door kinematics configuration")
    private DoorSpec doors;

    @Schema(description = "Engine technical specifications and exploded view breakdown")
    private EngineSpec engine;

    @Schema(description = "Interior cockpit and seat configuration")
    private InteriorSpec interior;

    @Schema(description = "Bespoke exterior paint options")
    private List<PaintOption> exteriorPaints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoorSpec {
        private String doorType;            // "SCISSOR", "BUTTERFLY", "GULLWING", "STANDARD"
        private Double maxOpenAngle;        // in radians or degrees (e.g. 1.2 rad ~ 70 deg)
        private List<String> doorMeshNames; // ["Door_L", "Door_R"]
        private Boolean supportsHood;
        private Boolean supportsTrunk;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EngineSpec {
        private String name;
        private String horsepower;
        private String torque;
        private String displacement;
        private String topSpeed;
        private String acceleration;
        private String engineType;          // "V12", "W16", "V8_HYBRID", "FLAT_6"
        private List<EngineComponent> components;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EngineComponent {
        private String id;
        private String name;
        private String description;
        private String spec;
        private String material;
        private Map<String, Double> explodedOffset; // { "x": 0.3, "y": 0.4, "z": 0.1 }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InteriorSpec {
        private String seatType;
        private List<String> upholsteryOptions;
        private List<ColorOption> seatColors;
        private Map<String, Double> cockpitCamera;  // { "x": 0.0, "y": 0.75, "z": 0.25 }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColorOption {
        private String id;
        private String name;
        private String hex;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaintOption {
        private String id;
        private String name;
        private String hex;
        private Double metallic;
        private Double roughness;
        private Double clearcoat;
    }
}
