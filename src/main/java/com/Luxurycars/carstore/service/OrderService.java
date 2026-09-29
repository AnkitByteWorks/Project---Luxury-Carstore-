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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CarRepository carRepository;
    private final OrderEventService orderEventService;
    private final EmailNotificationService emailNotificationService;
    private final InvoiceService invoiceService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        CarRepository carRepository,
                        @Autowired(required = false) OrderEventService orderEventService,
                        @Autowired(required = false) EmailNotificationService emailNotificationService,
                        @Autowired(required = false) InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
        this.orderEventService = orderEventService;
        this.emailNotificationService = emailNotificationService;
        this.invoiceService = invoiceService;
    }

    public OrderService(OrderRepository orderRepository,
                        CarRepository carRepository,
                        OrderEventService orderEventService) {
        this(orderRepository, carRepository, orderEventService, null, null);
    }

    public OrderService(OrderRepository orderRepository, CarRepository carRepository) {
        this(orderRepository, carRepository, null, null, null);
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
        // 2. Compute total = (price × quantity) + customPrice (if bespoke options selected)
        BigDecimal baseTotal = car.getPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()));
        BigDecimal customPrice = dto.getCustomPrice() != null ? dto.getCustomPrice() : BigDecimal.ZERO;
        BigDecimal total = baseTotal.add(customPrice);

        // 3. Build the order entity
        Order order = Order.builder()
                .carId(car.getId())
                .carName(car.getName())           // snapshot at order time
                .unitPrice(car.getPrice())        // snapshot at order time
                .quantity(dto.getQuantity())
                .totalAmount(total)
                .customOptions(dto.getCustomOptions())
                .customPrice(dto.getCustomPrice())
                .monogramText(dto.getMonogramText())
                .monogramColor(dto.getMonogramColor())
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

        // 5. Asynchronously dispatch luxury email notification with PDF invoice attachment
        if (emailNotificationService != null) {
            byte[] invoicePdf = null;
            if (invoiceService != null) {
                try {
                    invoicePdf = invoiceService.generateInvoice(saved);
                } catch (Exception ignored) {}
            }
            emailNotificationService.sendOrderConfirmationEmail(saved, invoicePdf);
        }

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
        OrderResponseDTO responseDTO = OrderMapper.toResponseDTO(updated);
        if (orderEventService != null) {
            orderEventService.publishOrderEvent(id, responseDTO);
        }
        return responseDTO;
    }

    // ─── CANCEL ORDER ───
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = findOrderOrThrow(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel a delivered order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        OrderResponseDTO responseDTO = OrderMapper.toResponseDTO(updated);
        if (orderEventService != null) {
            orderEventService.publishOrderEvent(id, responseDTO);
        }
        return responseDTO;
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

    public CarrierTelemetryDTO getCarrierTelemetry(Long orderId) {
        Order order = findOrderOrThrow(orderId);

        String city = order.getDeliveryCity() != null ? order.getDeliveryCity() : "Mumbai";
        String carrierId = "VIP-CARRIER-" + String.format("%03d", (order.getId() % 100) + 1);

        int progress;
        String waypoint;
        int speed;
        double temp = 21.4;
        String security = "Climate-Controlled Transporter • Air Suspension Active • Satellite Tracked";

        if (order.getStatus() == OrderStatus.DELIVERED) {
            progress = 100;
            waypoint = "Delivered to Private Residence (" + city + ")";
            speed = 0;
            security = "Delivery Complete • Client Received Keys in Bespoke Presentation Box";
        } else if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING) {
            progress = 68;
            waypoint = "En Route via NH-48 Expressway approaching " + city;
            speed = 78;
        } else {
            progress = 18;
            waypoint = "Pre-Transit Secure Preparation at Flagship Atelier (Mumbai)";
            speed = 0;
            security = "White-Glove PDI & Ceramic Shield Inspection Underway";
        }

        LocalDateTime eta = LocalDateTime.now().plusHours(3).plusMinutes(25);

        return CarrierTelemetryDTO.builder()
                .orderId(order.getId())
                .carrierId(carrierId)
                .carrierName("Carstore White-Glove Enclosed Carrier")
                .driverName("Vikram Rathore (Level 3 Armed Escort Specialist)")
                .currentWaypoint(waypoint)
                .transitSpeedKmH(speed)
                .trailerTempCelsius(temp)
                .progressPercent(progress)
                .estimatedArrival(eta)
                .destinationCity(city)
                .transportSecurityStatus(security)
                .build();
    }
}
