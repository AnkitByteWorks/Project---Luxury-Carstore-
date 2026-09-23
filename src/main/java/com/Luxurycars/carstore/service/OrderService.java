package com.Luxurycars.carstore.service;
import com.Luxurycars.carstore.dto.*;

import java.util.Map;
import java.util.stream.Collectors;

import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CarRepository carRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, CarRepository carRepository) {
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
    }

    public OrderStatsDTO getStats() {
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        return OrderStatsDTO.builder()
                .totalOrders(orders.size())
                .totalRevenue(totalRevenue)
                .pendingRevenue(pendingRevenue)
                .ordersByStatus(byStatus)
                .build();
    }

    // ─── PLACE A NEW ORDER ───
    public OrderResponseDTO placeOrder(OrderRequestDTO dto) {
        // 1. Find the car (or fail with 404)
        Car car = carRepository.findById(dto.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + dto.getCarId()));
        // 2. Compute total = price × quantity
        BigDecimal total = car.getPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()));

        // 3. Build the order entity
        Order order = Order.builder()
                .carId(car.getId())
                .carName(car.getName())           // snapshot at order time
                .unitPrice(car.getPrice())        // snapshot at order time
                .quantity(dto.getQuantity())
                .totalAmount(total)
                .customerName(dto.getCustomerName())
                .customerEmail(dto.getCustomerEmail())
                .customerPhone(dto.getCustomerPhone())
                .deliveryAddress(dto.getDeliveryAddress())
                .deliveryCity(dto.getDeliveryCity())
                .deliveryPincode(dto.getDeliveryPincode())
                .paymentMethod(dto.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .build();

        // 4. Save & return DTO
        Order saved = orderRepository.save(order);
        return OrderMapper.toResponseDTO(saved);
    }

    // ─── GET ONE ORDER ───
    public OrderResponseDTO getOrderById(Long id) {
        Order order = findOrderOrThrow(id);
        return OrderMapper.toResponseDTO(order);
    }

    // ─── GET ALL ORDERS ───
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── GET ORDERS FOR A SPECIFIC CAR ───
    public List<OrderResponseDTO> getOrdersByCar(Long carId) {
        return orderRepository.findByCarId(carId)
                .stream()
                .map(OrderMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── GET ORDERS BY CUSTOMER EMAIL ───
    public List<OrderResponseDTO> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmailIgnoreCase(email)
                .stream()
                .map(OrderMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── UPDATE ORDER STATUS (admin action) ───
    public OrderResponseDTO updateStatus(Long id, OrderStatus newStatus) {
        Order order = findOrderOrThrow(id);
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        return OrderMapper.toResponseDTO(updated);
    }

    // ─── CANCEL ORDER ───
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel a delivered order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return OrderMapper.toResponseDTO(updated);
    }

    // ─── HELPERS ───
    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
    public PageResponseDTO<OrderResponseDTO> getOrdersPaginated(
            int page, int size, String sortBy, String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        Page<OrderResponseDTO> dtoPage = orderPage.map(OrderMapper::toResponseDTO);
        return PageResponseDTO.from(dtoPage);
    }
}
