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
}