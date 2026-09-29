package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.Car3dSpecDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Car3dServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private Car3dService car3dService;

    private Car testCar;

    @BeforeEach
    void setUp() {
        testCar = new Car();
        testCar.setId(1L);
        testCar.setName("McLaren P1");
        testCar.setBrand("McLaren");
        testCar.setPrice(BigDecimal.valueOf(150000000));
        testCar.setDescription("Hybrid Hypercar with dihedral butterfly doors.");
    }

    @Test
    @DisplayName("Should return 3D specification and kinematics for car")
    void shouldReturn3dSpecForCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        Car3dSpecDTO spec = car3dService.get3dSpec(1L);

        assertThat(spec).isNotNull();
        assertThat(spec.getCarId()).isEqualTo(1L);
        assertThat(spec.getCarName()).isEqualTo("McLaren P1");
        assertThat(spec.getBrand()).isEqualTo("McLaren");
        assertThat(spec.getDoors()).isNotNull();
        assertThat(spec.getDoors().getDoorType()).isEqualTo("BUTTERFLY");
        assertThat(spec.getEngine()).isNotNull();
        assertThat(spec.getEngine().getComponents()).isNotEmpty();
        assertThat(spec.getInterior()).isNotNull();
        assertThat(spec.getInterior().getSeatColors()).isNotEmpty();
        assertThat(spec.getExteriorPaints()).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when car is not found")
    void shouldThrowExceptionWhenCarNotFound() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> car3dService.get3dSpec(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: 999");
    }
}
