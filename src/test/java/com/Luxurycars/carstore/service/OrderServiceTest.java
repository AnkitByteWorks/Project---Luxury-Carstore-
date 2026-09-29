package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.OrderRequestDTO;
import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CarRepository carRepository;

    @InjectMocks private OrderService orderService;

    private Car testCar;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
                .id(1L).name("Porsche 911").brand("Porsche")
                .price(new BigDecimal("28500000"))
                .build();
    }

    @Test
    @DisplayName("placeOrder - should compute total correctly")
    void placeOrder_shouldComputeTotal() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrderRequestDTO dto = OrderRequestDTO.builder()
                .carId(1L).quantity(2)
                .customerName("Ankit").customerEmail("a@x.com")
                .customerPhone("9876543210")
                .deliveryAddress("123 Street").deliveryCity("Mumbai").deliveryPincode("400001")
                .paymentMethod("EMI")
                .build();

        OrderResponseDTO result = orderService.placeOrder(dto);

        assertThat(result).isNotNull();
        assertThat(result.getUnitPrice()).isEqualByComparingTo("28500000");
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("57000000");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("placeOrder - should throw when car not found")
    void placeOrder_shouldThrow_whenCarNotFound() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        OrderRequestDTO dto = OrderRequestDTO.builder()
                .carId(999L).quantity(1)
                .customerName("X").customerEmail("x@x.com")
                .customerPhone("1234567890")
                .deliveryAddress("A").deliveryCity("B").deliveryPincode("123")
                .paymentMethod("Cash")
                .build();

        assertThatThrownBy(() -> orderService.placeOrder(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: 999");
    }

    @Test
    @DisplayName("cancelOrder - should throw when already delivered")
    void cancelOrder_shouldThrow_whenDelivered() {
        Order delivered = Order.builder()
                .id(1L).carId(1L).carName("Porsche")
                .unitPrice(new BigDecimal("28500000")).quantity(1)
                .totalAmount(new BigDecimal("28500000"))
                .customerName("X").customerEmail("x@x.com").customerPhone("1234567890")
                .deliveryAddress("A").deliveryCity("B").deliveryPincode("123")
                .paymentMethod("Cash")
                .status(OrderStatus.DELIVERED)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel a delivered order");
    }

    @Test
    @DisplayName("placeOrder - should include bespoke customization options and adjust total amount")
    void placeOrder_shouldIncludeBespokeCustomizationPrice() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(2L);
            return o;
        });

        OrderRequestDTO dto = OrderRequestDTO.builder()
                .carId(1L).quantity(1)
                .customerName("Ankit").customerEmail("a@x.com")
                .customerPhone("9876543210")
                .deliveryAddress("123 Street").deliveryCity("Mumbai").deliveryPincode("400001")
                .paymentMethod("UPI")
                .customOptions("21\" Forged Monoblock Wheels, Carbon Ceramic Brakes")
                .customPrice(new BigDecimal("1500000.00"))
                .build();

        OrderResponseDTO result = orderService.placeOrder(dto);

        assertThat(result).isNotNull();
        assertThat(result.getUnitPrice()).isEqualByComparingTo("28500000");
        assertThat(result.getCustomPrice()).isEqualByComparingTo("1500000.00");
        assertThat(result.getCustomOptions()).isEqualTo("21\" Forged Monoblock Wheels, Carbon Ceramic Brakes");
        // Total should be 28500000 + 1500000 = 30000000
        assertThat(result.getTotalAmount()).isEqualByComparingTo("30000000.00");
    }

    @Test
    @DisplayName("placeOrder - should save monogram text and color")
    void placeOrder_shouldIncludeMonogram() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(3L);
            return o;
        });

        OrderRequestDTO dto = OrderRequestDTO.builder()
                .carId(1L).quantity(1)
                .customerName("Lord Bruce").customerEmail("bruce@wayne.com")
                .customerPhone("9876543210")
                .deliveryAddress("Wayne Manor").deliveryCity("Gotham").deliveryPincode("400001")
                .paymentMethod("UPI")
                .monogramText("WAYNE • GOTHAM 01")
                .monogramColor("Amber Gold")
                .build();

        OrderResponseDTO result = orderService.placeOrder(dto);

        assertThat(result).isNotNull();
        assertThat(result.getMonogramText()).isEqualTo("WAYNE • GOTHAM 01");
        assertThat(result.getMonogramColor()).isEqualTo("Amber Gold");
    }

    @Test
    @DisplayName("getCarrierTelemetry - should return simulated live GPS logistics data")
    void getCarrierTelemetry_shouldReturnValidData() {
        Order confirmedOrder = Order.builder()
                .id(105L)
                .carId(1L)
                .carName("Porsche 911")
                .deliveryCity("Bangalore")
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(105L)).thenReturn(Optional.of(confirmedOrder));

        com.Luxurycars.carstore.dto.CarrierTelemetryDTO telemetry = orderService.getCarrierTelemetry(105L);

        assertThat(telemetry).isNotNull();
        assertThat(telemetry.getOrderId()).isEqualTo(105L);
        assertThat(telemetry.getCarrierId()).startsWith("VIP-CARRIER-");
        assertThat(telemetry.getProgressPercent()).isEqualTo(68);
        assertThat(telemetry.getTransitSpeedKmH()).isEqualTo(78);
        assertThat(telemetry.getDestinationCity()).isEqualTo("Bangalore");
        assertThat(telemetry.getEstimatedArrival()).isNotNull();
    }
}