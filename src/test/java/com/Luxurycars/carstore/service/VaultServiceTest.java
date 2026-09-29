package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.PasscodeVerificationResponseDTO;
import com.Luxurycars.carstore.dto.VaultInquiryRequestDTO;
import com.Luxurycars.carstore.dto.VaultInquiryResponseDTO;
import com.Luxurycars.carstore.entity.VaultAllocation;
import com.Luxurycars.carstore.entity.VaultInquiry;
import com.Luxurycars.carstore.repository.VaultAllocationRepository;
import com.Luxurycars.carstore.repository.VaultInquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultAllocationRepository allocationRepository;

    @Mock
    private VaultInquiryRepository inquiryRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private VaultService vaultService;

    private VaultAllocation testAllocation;

    @BeforeEach
    void setUp() {
        testAllocation = VaultAllocation.builder()
                .id("v-01")
                .name("Pagani Huayra R")
                .builder("Horacio Pagani Atelier")
                .chassisNumber("Chassis #14/30")
                .productionRun("1 of 30 Worldwide")
                .engineSpecs("6.0L V12")
                .horsepower(850)
                .topSpeedKmH(380)
                .price(new BigDecimal("350000000.00"))
                .imageUrl("https://example.com/huayra.jpg")
                .status("Private Allocation Available")
                .build();
    }

    @Test
    @DisplayName("Should accept valid VIP passcode and return token")
    void shouldAcceptValidPasscode() {
        when(jwtService.generateTokenForSubject(eq("VIP-CLIENT-CARSTOREVIP"), any(Map.class)))
                .thenReturn("mocked.jwt.token");

        PasscodeVerificationResponseDTO response = vaultService.verifyPasscode("CARSTOREVIP");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getTier()).contains("VIP ALLOCATION");
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
    }

    @Test
    @DisplayName("Should accept CENTURION passcode with Centurion tier")
    void shouldAcceptCenturionPasscode() {
        when(jwtService.generateTokenForSubject(eq("VIP-CLIENT-CENTURION"), any(Map.class)))
                .thenReturn("mocked.centurion.token");

        PasscodeVerificationResponseDTO response = vaultService.verifyPasscode("centurion");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getTier()).contains("CENTURION");
        assertThat(response.getToken()).isEqualTo("mocked.centurion.token");
    }

    @Test
    @DisplayName("Should reject invalid passcode")
    void shouldRejectInvalidPasscode() {
        PasscodeVerificationResponseDTO response = vaultService.verifyPasscode("INVALID_CODE");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("Denied");
    }

    @Test
    @DisplayName("Should return allocations from repository")
    void shouldReturnAllocations() {
        when(allocationRepository.findAll()).thenReturn(List.of(testAllocation));

        List<VaultAllocation> allocations = vaultService.getAllocations();

        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).getName()).isEqualTo("Pagani Huayra R");
    }

    @Test
    @DisplayName("Should submit confidential inquiry successfully")
    void shouldSubmitInquiry() {
        VaultInquiryRequestDTO request = VaultInquiryRequestDTO.builder()
                .allocationId("v-01")
                .clientName("Lord Mountbatten")
                .clientPhone("+919876543210")
                .build();

        VaultInquiry savedInquiry = VaultInquiry.builder()
                .id(101L)
                .allocationId("v-01")
                .clientName("Lord Mountbatten")
                .clientPhone("+919876543210")
                .createdAt(LocalDateTime.now())
                .build();

        when(inquiryRepository.save(any(VaultInquiry.class))).thenReturn(savedInquiry);

        VaultInquiryResponseDTO response = vaultService.submitInquiry(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getClientName()).isEqualTo("Lord Mountbatten");
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
    }
}
