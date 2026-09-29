package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.Car3dSpecDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class Car3dService {

    private final CarRepository carRepository;

    @Autowired
    public Car3dService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public Car3dSpecDTO get3dSpec(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

        String brand = car.getBrand() != null ? car.getBrand().toLowerCase() : "";
        String name = car.getName() != null ? car.getName().toLowerCase() : "";

        // Determine Door Type based on vehicle pedigree
        String doorType = "BUTTERFLY";
        if (brand.contains("lamborghini")) {
            doorType = "SCISSOR";
        } else if (brand.contains("mercedes") && name.contains("black")) {
            doorType = "GULLWING";
        } else if (brand.contains("porsche") || brand.contains("bmw") || brand.contains("audi")) {
            doorType = "STANDARD";
        }

        // Engine specifications tailored to vehicle
        Car3dSpecDTO.EngineSpec engineSpec;
        if (brand.contains("bugatti")) {
            engineSpec = Car3dSpecDTO.EngineSpec.builder()
                    .name("8.0L Quad-Turbocharged W16")
                    .horsepower("1,578 HP @ 7,000 RPM")
                    .torque("1,600 Nm @ 2,000–6,000 RPM")
                    .displacement("7,993 cc")
                    .topSpeed("440 km/h (273 mph)")
                    .acceleration("0–100 km/h in 2.4s")
                    .engineType("W16")
                    .components(List.of(
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("turbo_primary")
                                    .name("Sequential Twin-Scroll Turbos (x4)")
                                    .spec("Titanium-aluminide turbine wheels, 1.8 bar boost")
                                    .material("Aerospace Grade Titanium")
                                    .description("Two-stage sequential turbochargers feeding over 60,000 liters of air per minute into the W16 block.")
                                    .explodedOffset(Map.of("x", 0.35, "y", 0.20, "z", 0.15))
                                    .build(),
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("intake_manifold")
                                    .name("Carbon Fiber Intake Plenum")
                                    .spec("Full dry-carbon dual airbox")
                                    .material("Pre-preg Carbon Fiber")
                                    .description("Ultra-rigid lightweight carbon induction manifold tuned for optimal acoustic resonance.")
                                    .explodedOffset(Map.of("x", 0.0, "y", 0.35, "z", 0.0))
                                    .build(),
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("cylinder_heads")
                                    .name("Dual Monoblock Cylinder Heads (64-Valve)")
                                    .spec("4 valves per cylinder, forged titanium valvetrain")
                                    .material("Heat-treated Aluminium Alloy")
                                    .description("Engineered with twin dual-overhead camshafts capable of spinning up to 9,000 RPM safely.")
                                    .explodedOffset(Map.of("x", -0.30, "y", 0.15, "z", -0.10))
                                    .build(),
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("exhaust_system")
                                    .name("Inconel Quad Exhaust System")
                                    .spec("Formula 1 grade Inconel alloy, titanium tips")
                                    .material("Inconel 625 Superalloy")
                                    .description("Extreme thermal resistance with ceramic thermal coating to withstand 1,000°C exhaust gas temperatures.")
                                    .explodedOffset(Map.of("x", 0.0, "y", -0.15, "z", -0.40))
                                    .build()
                    ))
                    .build();
        } else if (brand.contains("ferrari")) {
            engineSpec = Car3dSpecDTO.EngineSpec.builder()
                    .name("3.9L Twin-Turbo V8 + Tri-Motor Hybrid")
                    .horsepower("986 HP Combined Output")
                    .torque("800 Nm ICE + 390 Nm Electric")
                    .displacement("3,990 cc")
                    .topSpeed("340 km/h (211 mph)")
                    .acceleration("0–100 km/h in 2.5s")
                    .engineType("V8_HYBRID")
                    .components(List.of(
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("turbo_assembly")
                                    .name("IHI Twin-Scroll Ball Bearing Turbos")
                                    .spec("Electronic wastegates with 350-bar direct injection")
                                    .material("Inconel / Titanium")
                                    .description("Mounted low and close to the crankshaft for lightning-fast throttle response.")
                                    .explodedOffset(Map.of("x", 0.28, "y", 0.18, "z", 0.12))
                                    .build(),
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("hybrid_mgu")
                                    .name("MGU-K Electric Axial Motors")
                                    .spec("220 HP regenerative electric front-axle drive")
                                    .material("Copper Core & Neodymium")
                                    .description("Provides instant torque-fill and all-wheel-drive torque vectoring through turns.")
                                    .explodedOffset(Map.of("x", 0.0, "y", 0.30, "z", 0.25))
                                    .build()
                    ))
                    .build();
        } else {
            engineSpec = Car3dSpecDTO.EngineSpec.builder()
                    .name("Naturally Aspirated 6.5L V12")
                    .horsepower("770 HP @ 8,500 RPM")
                    .torque("720 Nm @ 6,750 RPM")
                    .displacement("6,498 cc")
                    .topSpeed("355 km/h (220 mph)")
                    .acceleration("0–100 km/h in 2.8s")
                    .engineType("V12")
                    .components(List.of(
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("v12_block")
                                    .name("60° Aluminum-Silicon V12 Engine Block")
                                    .spec("Dry-sump lubrication with scavenge pumps")
                                    .material("Aluminium-Silicon Alloy")
                                    .description("High-revving naturally aspirated V12 with a short stroke design for instant RPM response.")
                                    .explodedOffset(Map.of("x", 0.0, "y", 0.25, "z", 0.0))
                                    .build(),
                            Car3dSpecDTO.EngineComponent.builder()
                                    .id("carbon_intake")
                                    .name("Multi-Stage Variable Geometry Intake")
                                    .spec("Bespoke intake runners with butterfly throttles")
                                    .material("Carbon Fiber Composite")
                                    .description("Dynamically alters runner length for maximum volumetric efficiency at high revs.")
                                    .explodedOffset(Map.of("x", 0.25, "y", 0.20, "z", 0.15))
                                    .build()
                    ))
                    .build();
        }

        // Interior & Seats Spec
        Car3dSpecDTO.InteriorSpec interiorSpec = Car3dSpecDTO.InteriorSpec.builder()
                .seatType("Carbon Fiber Shell Bucket Seats")
                .upholsteryOptions(List.of("Semi-Aniline Nappa Leather", "Race Alcantara", "Bespoke Duo-Tone Stitching"))
                .seatColors(List.of(
                        Car3dSpecDTO.ColorOption.builder().id("saddle_tan").name("Hermès Saddle Tan").hex("#c97a3e").build(),
                        Car3dSpecDTO.ColorOption.builder().id("nero_black").name("Nero Black Alcantara").hex("#1e2124").build(),
                        Car3dSpecDTO.ColorOption.builder().id("rosso_red").name("Rosso Monza Red").hex("#9e1b1b").build(),
                        Car3dSpecDTO.ColorOption.builder().id("crema_ivory").name("Crema White Nappa").hex("#e8dec8").build()
                ))
                .cockpitCamera(Map.of("x", 0.0, "y", 0.75, "z", 0.20))
                .build();

        // Bespoke Exterior Paints
        List<Car3dSpecDTO.PaintOption> paints = List.of(
                Car3dSpecDTO.PaintOption.builder().id("carbon_blue").name("Exposed Royal Blue Carbon").hex("#0f2b5c").metallic(0.85).roughness(0.18).clearcoat(1.0).build(),
                Car3dSpecDTO.PaintOption.builder().id("rosso_corsa").name("Rosso Corsa Crimson").hex("#b91c1c").metallic(0.70).roughness(0.20).clearcoat(1.0).build(),
                Car3dSpecDTO.PaintOption.builder().id("nero_daytona").name("Nero Daytona Metallic").hex("#0f172a").metallic(0.95).roughness(0.15).clearcoat(1.0).build(),
                Car3dSpecDTO.PaintOption.builder().id("giallo_gold").name("Giallo Modena Gold").hex("#d97706").metallic(0.80).roughness(0.22).clearcoat(1.0).build(),
                Car3dSpecDTO.PaintOption.builder().id("argento_silver").name("Liquid Argento Silver").hex("#cbd5e1").metallic(0.98).roughness(0.12).clearcoat(1.0).build()
        );

        return Car3dSpecDTO.builder()
                .carId(car.getId())
                .carName(car.getName())
                .brand(car.getBrand())
                .model3dUrl("/api/cars/" + car.getId() + "/3d-model")
                .doors(Car3dSpecDTO.DoorSpec.builder()
                        .doorType(doorType)
                        .maxOpenAngle(1.2)
                        .doorMeshNames(List.of("Door_Left", "Door_Right"))
                        .supportsHood(true)
                        .supportsTrunk(true)
                        .build())
                .engine(engineSpec)
                .interior(interiorSpec)
                .exteriorPaints(paints)
                .build();
    }
}
