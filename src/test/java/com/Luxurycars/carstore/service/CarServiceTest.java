package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.CarRequestDTO;
import com.Luxurycars.carstore.dto.CarResponseDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // ← enables Mockito annotations
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private CarService carService;

    private Car testCar;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
                .id(1L)
                .name("Porsche 911 Turbo S")
                .brand("Porsche")
                .price(new BigDecimal("28500000.00"))
                .description("Iconic sports car")
                .colorOptions("Silver,Red")
                .showroomLocation("Mumbai")
                .deliveryDays(45)
                .paymentOptions("Cash,EMI")
                .build();
    }

    // ═══════════════════════════════════════════════
    //  getCarById
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("getCarById - should return DTO when car exists")
    void getCarById_shouldReturnDto_whenCarExists() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        CarResponseDTO result = carService.getCarById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Porsche 911 Turbo S");
        assertThat(result.getBrand()).isEqualTo("Porsche");

        verify(carRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getCarById - should throw when car not found")
    void getCarById_shouldThrow_whenCarNotFound() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.getCarById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: 999");

        verify(carRepository, times(1)).findById(999L);
    }

    // ═══════════════════════════════════════════════
    //  createCar
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("createCar - should save and return DTO")
    void createCar_shouldSaveAndReturnDto() {
        CarRequestDTO dto = CarRequestDTO.builder()
                .name("Test Car")
                .brand("Test Brand")
                .price(new BigDecimal("5000000"))
                .build();

        Car savedCar = Car.builder()
                .id(10L)
                .name("Test Car")
                .brand("Test Brand")
                .price(new BigDecimal("5000000"))
                .build();

        when(carRepository.save(any(Car.class))).thenReturn(savedCar);

        CarResponseDTO result = carService.createCar(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Test Car");

        verify(carRepository, times(1)).save(any(Car.class));
    }

    // ═══════════════════════════════════════════════
    //  deleteCar
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("deleteCar - should delete existing car")
    void deleteCar_shouldDelete_whenCarExists() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        carService.deleteCar(1L);

        verify(carRepository, times(1)).delete(testCar);
    }

    @Test
    @DisplayName("deleteCar - should throw when car not found")
    void deleteCar_shouldThrow_whenCarNotFound() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.deleteCar(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ═══════════════════════════════════════════════
    //  searchCars
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("searchCars - should return matching cars")
    void searchCars_shouldReturnMatches() {
        when(carRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase("porsche", "porsche"))
                .thenReturn(List.of(testCar));

        var results = carService.searchCars("porsche");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBrand()).isEqualTo("Porsche");
    }
}