package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.PasscodeVerificationRequestDTO;
import com.Luxurycars.carstore.dto.PasscodeVerificationResponseDTO;
import com.Luxurycars.carstore.dto.VaultInquiryRequestDTO;
import com.Luxurycars.carstore.dto.VaultInquiryResponseDTO;
import com.Luxurycars.carstore.entity.VaultAllocation;
import com.Luxurycars.carstore.service.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vault")
@Tag(name = "Secret Vault", description = "VIP Tier Passcode Verification & Confidential Hypercar Allocations")
public class VaultController {

    private final VaultService vaultService;

    @Autowired
    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Operation(summary = "Verify VIP Passcode for Secret Vault Access")
    @PostMapping("/verify-passcode")
    public ResponseEntity<PasscodeVerificationResponseDTO> verifyPasscode(
            @Valid @RequestBody PasscodeVerificationRequestDTO request) {

        PasscodeVerificationResponseDTO response = vaultService.verifyPasscode(request.getPasscode());
        if (!response.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all confidential 1-of-1 hypercar build slots")
    @GetMapping("/allocations")
    public ResponseEntity<List<VaultAllocation>> getAllocations() {
        return ResponseEntity.ok(vaultService.getAllocations());
    }

    @Operation(summary = "Submit confidential VIP allocation inquiry")
    @PostMapping("/inquire")
    public ResponseEntity<VaultInquiryResponseDTO> inquire(
            @Valid @RequestBody VaultInquiryRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.submitInquiry(request));
    }
}
