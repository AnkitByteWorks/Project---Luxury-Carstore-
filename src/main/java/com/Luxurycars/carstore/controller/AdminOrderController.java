package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.OrderResponseDTO;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin Orders", description = "Administrative operations for luxury car orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @Autowired
    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Update order status as Admin and broadcast SSE event",
            description = "Admin endpoint to transition order status (PENDING -> PROCESSING -> CONFIRMED -> SHIPPED -> DELIVERED)")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus value) {

        OrderResponseDTO updated = orderService.updateStatus(id, value);
        return ResponseEntity.ok(updated);
    }
}
