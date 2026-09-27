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

import com.Luxurycars.carstore.service.InvoiceService;
import com.Luxurycars.carstore.service.OrderEventService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Operations related to luxury car orders, tax invoices, and real-time live tracking")
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final OrderEventService orderEventService;

    @Autowired
    public OrderController(OrderService orderService,
                           InvoiceService invoiceService,
                           OrderEventService orderEventService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.orderEventService = orderEventService;
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

    // ────────────────────────────────────────────────
    // 9. OFFICIAL PDF TAX INVOICE GENERATION
    // GET /api/orders/1/invoice
    // ────────────────────────────────────────────────
    @Operation(summary = "Download official PDF tax invoice", description = "Generates a luxury branded PDF tax invoice (Authorized for order owner or ADMIN)")
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoice(
            @PathVariable Long id,
            Authentication authentication) {

        byte[] pdf = invoiceService.generateInvoicePdf(id, authentication);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("Carstore-Invoice-ORD-" + id + ".pdf")
                .build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ────────────────────────────────────────────────
    // 10. REAL-TIME ORDER STATUS SSE STREAM
    // GET /api/orders/1/events
    // ────────────────────────────────────────────────
    @Operation(summary = "Subscribe to live order status SSE updates", description = "Server-Sent Events stream emitting live order status changes")
    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToOrderEvents(@PathVariable Long id) {
        return orderEventService.subscribe(id);
    }
}