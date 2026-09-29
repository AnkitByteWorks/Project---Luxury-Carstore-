package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.Car3dSpecDTO;
import com.Luxurycars.carstore.service.Car3dService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
@Tag(name = "Car 3D Experience", description = "Endpoints for real-time 3D vehicle visualizer, door kinematics, and engine inspection")
public class Car3dController {

    private final Car3dService car3dService;

    @Autowired
    public Car3dController(Car3dService car3dService) {
        this.car3dService = car3dService;
    }

    @Operation(summary = "Get 3D specification, door kinematics, and engine component breakdown for a car")
    @GetMapping("/{id}/3d-spec")
    public ResponseEntity<Car3dSpecDTO> get3dSpec(@PathVariable Long id) {
        return ResponseEntity.ok(car3dService.get3dSpec(id));
    }
}
