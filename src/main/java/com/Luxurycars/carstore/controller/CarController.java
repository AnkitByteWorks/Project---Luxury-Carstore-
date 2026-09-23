package com.Luxurycars.carstore.controller;
import com.Luxurycars.carstore.dto.PageResponseDTO;
import com.Luxurycars.carstore.dto.CarRequestDTO;
import com.Luxurycars.carstore.dto.CarResponseDTO;
import com.Luxurycars.carstore.dto.CarStatsDTO;
import com.Luxurycars.carstore.service.CarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cars")

@Tag(name = "Cars", description = "Operations related to luxury cars")
@Validated
public class CarController {

    private final CarService carService;


    @Autowired
    public CarController(CarService carService) {
        this.carService = carService;
    }

    // ─── STATS ───
    @GetMapping("/stats")
    public ResponseEntity<CarStatsDTO> getStats() {
        return ResponseEntity.ok(carService.getStats());
    }

    // ─── FEATURED CARS (5 most expensive) ───
    @GetMapping("/featured")
    public ResponseEntity<List<CarResponseDTO>> getFeatured() {
        return ResponseEntity.ok(carService.getFeaturedCars());
    }

    // ─── LATEST CARS (5 most recently added) ───
    @GetMapping("/latest")
    public ResponseEntity<List<CarResponseDTO>> getLatest() {
        return ResponseEntity.ok(carService.getLatestCars());
    }

    // ─── GET ALL (returns DTOs, NOT entities) ───
    @Operation(summary = "Get all cars (paginated + sorted)",
            description = "Example: /api/cars?page=0&size=10&sortBy=price&direction=desc")
    @GetMapping
    public ResponseEntity<PageResponseDTO<CarResponseDTO>> getAllCars(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be >= 0")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 100, message = "Size must be <= 100")
            int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(carService.getCarsPaginated(page, size, sortBy, direction));
    }

    // ─── GET ONE ───
    @Operation(summary = "Get a car by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CarResponseDTO> getCarById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }

    // ─── CREATE (accepts CarRequestDTO, validated) ───
    @Operation(summary = "Create a new car (Admin only)")
    @PostMapping
    public ResponseEntity<CarResponseDTO> createCar(@Valid @RequestBody CarRequestDTO dto) {
        //       ↑ @Valid triggers the @NotBlank / @Positive checks
        CarResponseDTO saved = carService.createCar(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // ─── UPDATE ───
    @Operation(summary = "Update a car")
    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDTO> updateCar(
            @PathVariable Long id,
            @Valid @RequestBody CarRequestDTO dto) {
        return ResponseEntity.ok(carService.updateCar(id, dto));
    }

    // ─── DELETE ───
    @Operation(summary = "Delete a car")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.ok("Car with id " + id + " deleted successfully.");
    }

    // ─── SEARCH ───
    @Operation(summary = "Search cars by name or brand")
    @GetMapping("/search")
    public ResponseEntity<List<CarResponseDTO>> searchCars(@RequestParam String keyword) {
        return ResponseEntity.ok(carService.searchCars(keyword));
    }

    // ─── FILTER BY BRAND ───
    @Operation(summary = "Filter cars by brand")
    @GetMapping("/brand")
    public ResponseEntity<List<CarResponseDTO>> getByBrand(@RequestParam String name) {
        return ResponseEntity.ok(carService.getCarsByBrand(name));
    }

    // ─── FILTER BY PRICE ───
    @Operation(summary = "Filter cars by price range")
    @GetMapping("/price")
    public ResponseEntity<List<CarResponseDTO>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(carService.getCarsByPriceRange(min, max));
    }

    // ─── FILTER BY LOCATION ───
    @Operation(summary = "Filter cars by showroom location")
    @GetMapping("/location")
    public ResponseEntity<List<CarResponseDTO>> getByLocation(@RequestParam String city) {
        return ResponseEntity.ok(carService.getCarsByLocation(city));
    }

    // ─── IMAGE UPLOAD ───
    @Operation(summary = "Upload an image for a car")
    @PostMapping("/{id}/image")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        carService.uploadImage(id, file);
        return ResponseEntity.ok("Image uploaded for car id " + id);
    }

    // ─── IMAGE FETCH (still returns raw bytes) ───
    @Operation(summary = "Get the image bytes for a car")
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        byte[] image = carService.getCarImage(id);

        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Get content type from DB
        var car = carService.getCarById(id);
        String contentType = car.getImageName() != null
                ? determineContentType(car.getImageName())
                : "image/jpeg";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setCacheControl("max-age=86400");  // cache 24h

        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }


}