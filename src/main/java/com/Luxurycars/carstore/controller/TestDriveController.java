package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.TestDriveRequestDTO;
import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import com.Luxurycars.carstore.service.TestDriveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-drives")
@Tag(name = "Test Drives", description = "VIP luxury vehicle test drive booking and concierge management")
@Validated
public class TestDriveController {

    private final TestDriveService testDriveService;

    @Autowired
    public TestDriveController(TestDriveService testDriveService) {
        this.testDriveService = testDriveService;
    }

    @Operation(summary = "Book a VIP test drive", description = "Public/authenticated endpoint to book a showroom or doorstep test drive")
    @PostMapping
    public ResponseEntity<TestDriveResponseDTO> bookTestDrive(@Valid @RequestBody TestDriveRequestDTO dto) {
        TestDriveResponseDTO response = testDriveService.createTestDrive(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all test drive bookings (Admin only)", description = "Admin view to inspect all VIP appointments")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TestDriveResponseDTO>> getAllTestDrives() {
        return ResponseEntity.ok(testDriveService.getAllTestDrives());
    }

    @Operation(summary = "Lookup test drive by reference code", description = "Lookup test drive appointment details by reference code")
    @GetMapping("/ref/{referenceCode}")
    public ResponseEntity<TestDriveResponseDTO> getByReferenceCode(@PathVariable String referenceCode) {
        return ResponseEntity.ok(testDriveService.getByReferenceCode(referenceCode));
    }

    @Operation(summary = "Get Amazon-style VIP fulfillment tracking timeline", description = "Live timeline tracking with logistics, checkpoint, and concierge details")
    @GetMapping("/{referenceCode}/tracking")
    public ResponseEntity<com.Luxurycars.carstore.dto.TestDriveTrackingDTO> getTrackingTimeline(@PathVariable String referenceCode) {
        return ResponseEntity.ok(testDriveService.getTrackingTimeline(referenceCode));
    }

    @Operation(summary = "Update test drive status (Admin only)", description = "Update the appointment status (PENDING, CONFIRMED, CONCIERGE_ASSIGNED, CARRIER_DISPATCHED, COMPLETED, CANCELLED)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestDriveResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam TestDriveStatus status) {
        return ResponseEntity.ok(testDriveService.updateStatus(id, status));
    }
}
