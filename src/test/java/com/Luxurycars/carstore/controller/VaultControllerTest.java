package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.CacheConfig;
import com.Luxurycars.carstore.config.JwtAuthFilter;
import com.Luxurycars.carstore.config.RateLimitFilter;
import com.Luxurycars.carstore.config.SecurityConfig;
import com.Luxurycars.carstore.dto.PasscodeVerificationRequestDTO;
import com.Luxurycars.carstore.dto.PasscodeVerificationResponseDTO;
import com.Luxurycars.carstore.dto.VaultInquiryRequestDTO;
import com.Luxurycars.carstore.dto.VaultInquiryResponseDTO;
import com.Luxurycars.carstore.entity.VaultAllocation;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
import com.Luxurycars.carstore.service.VaultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VaultController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import({SecurityConfig.class, JwtAuthFilter.class, CacheConfig.class})
@ActiveProfiles("test")
class VaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private VaultService vaultService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/vault/verify-passcode - Success")
    void shouldVerifyPasscodeSuccessfully() throws Exception {
        PasscodeVerificationRequestDTO request = PasscodeVerificationRequestDTO.builder()
                .passcode("CARSTOREVIP")
                .build();

        PasscodeVerificationResponseDTO response = PasscodeVerificationResponseDTO.builder()
                .valid(true)
                .tier("VIP ALLOCATION TIER")
                .token("mocked.vip.jwt")
                .message("Access Granted")
                .build();

        when(vaultService.verifyPasscode("CARSTOREVIP")).thenReturn(response);

        mockMvc.perform(post("/api/vault/verify-passcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.token").value("mocked.vip.jwt"))
                .andExpect(jsonPath("$.tier").value("VIP ALLOCATION TIER"));
    }

    @Test
    @DisplayName("POST /api/vault/verify-passcode - Unauthorized when invalid")
    void shouldReturnUnauthorizedForInvalidPasscode() throws Exception {
        PasscodeVerificationRequestDTO request = PasscodeVerificationRequestDTO.builder()
                .passcode("WRONG_CODE")
                .build();

        PasscodeVerificationResponseDTO response = PasscodeVerificationResponseDTO.builder()
                .valid(false)
                .message("Invalid VIP passcode")
                .build();

        when(vaultService.verifyPasscode("WRONG_CODE")).thenReturn(response);

        mockMvc.perform(post("/api/vault/verify-passcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("GET /api/vault/allocations - Returns allocations list")
    void shouldReturnAllocationsList() throws Exception {
        VaultAllocation allocation = VaultAllocation.builder()
                .id("v-01")
                .name("Pagani Huayra R")
                .builder("Horacio Pagani")
                .chassisNumber("Chassis #14/30")
                .productionRun("1 of 30")
                .engineSpecs("6.0L V12")
                .horsepower(850)
                .topSpeedKmH(380)
                .price(new BigDecimal("350000000.00"))
                .imageUrl("https://example.com/huayra.jpg")
                .status("Available")
                .build();

        when(vaultService.getAllocations()).thenReturn(List.of(allocation));

        mockMvc.perform(get("/api/vault/allocations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("v-01"))
                .andExpect(jsonPath("$[0].name").value("Pagani Huayra R"));
    }

    @Test
    @DisplayName("POST /api/vault/inquire - Submits confidential request")
    void shouldSubmitInquiry() throws Exception {
        VaultInquiryRequestDTO request = VaultInquiryRequestDTO.builder()
                .allocationId("v-01")
                .clientName("Wayne Enterprises")
                .clientPhone("+919999988888")
                .build();

        VaultInquiryResponseDTO response = VaultInquiryResponseDTO.builder()
                .id(1L)
                .allocationId("v-01")
                .clientName("Wayne Enterprises")
                .status("RECEIVED")
                .message("Inquiry submitted")
                .createdAt(LocalDateTime.now())
                .build();

        when(vaultService.submitInquiry(any(VaultInquiryRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/vault/inquire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.clientName").value("Wayne Enterprises"));
    }
}
