package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.PasscodeVerificationResponseDTO;
import com.Luxurycars.carstore.dto.VaultInquiryRequestDTO;
import com.Luxurycars.carstore.dto.VaultInquiryResponseDTO;
import com.Luxurycars.carstore.entity.VaultAllocation;
import com.Luxurycars.carstore.entity.VaultInquiry;
import com.Luxurycars.carstore.repository.VaultAllocationRepository;
import com.Luxurycars.carstore.repository.VaultInquiryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VaultService {

    private final VaultAllocationRepository allocationRepository;
    private final VaultInquiryRepository inquiryRepository;
    private final JwtService jwtService;

    @Autowired
    public VaultService(VaultAllocationRepository allocationRepository,
                        VaultInquiryRepository inquiryRepository,
                        JwtService jwtService) {
        this.allocationRepository = allocationRepository;
        this.inquiryRepository = inquiryRepository;
        this.jwtService = jwtService;
    }

    public PasscodeVerificationResponseDTO verifyPasscode(String passcode) {
        if (passcode == null || passcode.trim().isEmpty()) {
            return PasscodeVerificationResponseDTO.builder()
                    .valid(false)
                    .message("VIP Passcode is required")
                    .build();
        }

        String normalized = passcode.trim().toUpperCase();
        String tier;
        if ("CENTURION".equals(normalized)) {
            tier = "CENTURION BLACK TIER";
        } else if ("BLACKCARD".equals(normalized)) {
            tier = "BLACK CARD ELITE TIER";
        } else if ("CARSTOREVIP".equals(normalized)) {
            tier = "VIP ALLOCATION TIER";
        } else {
            return PasscodeVerificationResponseDTO.builder()
                    .valid(false)
                    .message("Invalid VIP passcode. Access to Confidential Vault Denied.")
                    .build();
        }

        String token = jwtService.generateTokenForSubject("VIP-CLIENT-" + normalized, Map.of(
                "tier", tier,
                "scope", "VAULT_ACCESS"
        ));

        log.info("💎 VIP Passcode accepted! Granted {} access with token", tier);

        return PasscodeVerificationResponseDTO.builder()
                .valid(true)
                .tier(tier)
                .token(token)
                .message("Confidential Secret Vault Access Granted. Welcome, Esteemed Collector.")
                .build();
    }

    public List<VaultAllocation> getAllocations() {
        List<VaultAllocation> allocations = allocationRepository.findAll();
        if (allocations.isEmpty()) {
            return seedDefaultAllocations();
        }
        return allocations;
    }

    @Transactional
    public VaultInquiryResponseDTO submitInquiry(VaultInquiryRequestDTO request) {
        VaultInquiry inquiry = VaultInquiry.builder()
                .allocationId(request.getAllocationId())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .createdAt(LocalDateTime.now())
                .build();

        VaultInquiry saved = inquiryRepository.save(inquiry);

        log.info("🔒 Confidential Vault Allocation Inquiry submitted: ID #{} for Lot {} by Client {}",
                saved.getId(), saved.getAllocationId(), saved.getClientName());

        return VaultInquiryResponseDTO.builder()
                .id(saved.getId())
                .allocationId(saved.getAllocationId())
                .clientName(saved.getClientName())
                .status("RECEIVED")
                .message("Your confidential allocation inquiry has been submitted. Our Managing Director will contact you via private line within 15 minutes.")
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public List<VaultAllocation> seedDefaultAllocations() {
        List<VaultAllocation> defaults = List.of(
                VaultAllocation.builder()
                        .id("v-01")
                        .name("Pagani Huayra R")
                        .builder("Horacio Pagani Atelier (San Cesario sul Panaro)")
                        .chassisNumber("Chassis #14/30 • Carbo-Titanium HP62 G2")
                        .productionRun("1 of 30 Worldwide")
                        .engineSpecs("6.0L Naturally Aspirated V12 (HWA AG)")
                        .horsepower(850)
                        .topSpeedKmH(380)
                        .price(new BigDecimal("350000000.00"))
                        .imageUrl("https://images.unsplash.com/photo-1544829099-b9a0c07fad1a?auto=format&fit=crop&w=1200&q=80")
                        .status("Private Allocation Available")
                        .build(),
                VaultAllocation.builder()
                        .id("v-02")
                        .name("Koenigsegg Jesko Absolut")
                        .builder("Koenigsegg Automotive (Ängelholm, Sweden)")
                        .chassisNumber("Chassis #007 • Low-Drag High-Speed Monocoque")
                        .productionRun("Strictly Limited Production")
                        .engineSpecs("5.0L Twin-Turbo Flat-Plane V8 (E85 Capable)")
                        .horsepower(1600)
                        .topSpeedKmH(531)
                        .price(new BigDecimal("420000000.00"))
                        .imageUrl("https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?auto=format&fit=crop&w=1200&q=80")
                        .status("Build Slot #04 Reserved for Delivery")
                        .build(),
                VaultAllocation.builder()
                        .id("v-03")
                        .name("Aston Martin Valkyrie AMR Pro")
                        .builder("Aston Martin Performance Technologies (Gaydon)")
                        .chassisNumber("Chassis #22/40 • Full Carbon Aerocell")
                        .productionRun("1 of 40 Worldwide")
                        .engineSpecs("6.5L Naturally Aspirated Cosworth V12")
                        .horsepower(1000)
                        .topSpeedKmH(362)
                        .price(new BigDecimal("385000000.00"))
                        .imageUrl("https://images.unsplash.com/photo-1621135802920-133df287f89c?auto=format&fit=crop&w=1200&q=80")
                        .status("Final Chassis Available")
                        .build(),
                VaultAllocation.builder()
                        .id("v-04")
                        .name("Bugatti Bolide Track Homologation")
                        .builder("Bugatti Atelier (Molsheim, France)")
                        .chassisNumber("Chassis #03/40 • FIA LMH Carbon Monocoque")
                        .productionRun("1 of 40 Worldwide")
                        .engineSpecs("8.0L Quad-Turbo W16 (110 Octane Race Fuel)")
                        .horsepower(1850)
                        .topSpeedKmH(500)
                        .price(new BigDecimal("440000000.00"))
                        .imageUrl("https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1200&q=80")
                        .status("Confidential Slot Inquire Only")
                        .build()
        );
        return allocationRepository.saveAll(defaults);
    }
}
