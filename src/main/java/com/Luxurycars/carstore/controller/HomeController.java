package com.Luxurycars.carstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Luxury Cars E-Commerce Backend API",
                "version", "0.0.1-SNAPSHOT",
                "swagger", "/swagger-ui.html",
                "apiDocs", "/v3/api-docs",
                "health", "/actuator/health",
                "cars", "/api/cars",
                "trending", "/api/cars/trending"
        ));
    }
}
