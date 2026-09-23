package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.OrderRequestDTO;
import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.dto.OrderStatsDTO;
import com.Luxurycars.carstore.dto.PageResponseDTO;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")

@Tag(name = "Orders", description = "Operations related to car orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ────────────────────────────────────────────────
    // 1. GET ORDER STATISTICS
    // GET /api/orders/stats
    // ────────────────────────────────────────────────
    @Operation(summary = "Get order statistics")
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsDTO> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }

    // ────────────────────────────────────────────────
    // 2. PLACE AN ORDER
    // POST /api/orders
    // ────────────────────────────────────────────────
    @Operation(summary = "Place a new order")
    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(
            @Valid @RequestBody OrderRequestDTO dto) {

        OrderResponseDTO order = orderService.placeOrder(dto);

        return new ResponseEntity<>(
                order,
                HttpStatus.CREATED
        );
    }

    // ────────────────────────────────────────────────
    // 3. GET ONE ORDER
    // GET /api/orders/1
    // ────────────────────────────────────────────────
    @Operation(summary = "Get an order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.getOrderById(id)
        );
    }

    // ────────────────────────────────────────────────
    // 4. GET ALL ORDERS
    // GET /api/orders
    // ────────────────────────────────────────────────
    @Operation(summary = "Get all orders (paginated)")
    @GetMapping
    public ResponseEntity<PageResponseDTO<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "orderedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                orderService.getOrdersPaginated(page, size, sortBy, direction));
    }

    // ────────────────────────────────────────────────
    // 5. GET ORDERS BY CAR
    // GET /api/orders/car/1
    // ────────────────────────────────────────────────
    @Operation(summary = "Get orders by car ID")
    @GetMapping("/car/{carId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByCar(
            @PathVariable Long carId) {

        return ResponseEntity.ok(
                orderService.getOrdersByCar(carId)
        );
    }

    // ────────────────────────────────────────────────
    // 6. GET ORDERS BY CUSTOMER EMAIL
    // GET /api/orders/customer?email=abc@test.com
    // ────────────────────────────────────────────────
    @Operation(summary = "Get orders by customer email")
    @GetMapping("/customer")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(
                orderService.getOrdersByEmail(email)
        );
    }

    // ────────────────────────────────────────────────
    // 7. UPDATE ORDER STATUS
    // PUT /api/orders/1/status?value=CONFIRMED
    // ────────────────────────────────────────────────
    @Operation(summary = "Update order status")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus value) {

        return ResponseEntity.ok(
                orderService.updateStatus(id, value)
        );
    }

    // ────────────────────────────────────────────────
    // 8. CANCEL ORDER
    // PUT /api/orders/1/cancel
    // ────────────────────────────────────────────────
    @Operation(summary = "Cancel an order")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.cancelOrder(id)
        );
    }
}