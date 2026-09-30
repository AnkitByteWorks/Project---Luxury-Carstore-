package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.RateLimitFilter;
import com.Luxurycars.carstore.dto.TestDriveRequestDTO;
import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.dto.TestDriveTrackingDTO;
import com.Luxurycars.carstore.entity.ExperienceType;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
import com.Luxurycars.carstore.service.TestDriveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TestDriveController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@org.springframework.context.annotation.Import({com.Luxurycars.carstore.config.SecurityConfig.class, com.Luxurycars.carstore.config.JwtAuthFilter.class, com.Luxurycars.carstore.config.CacheConfig.class})
@ActiveProfiles("test")
class TestDriveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private TestDriveService testDriveService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/test-drives - should book VIP test drive")
    void bookTestDrive_shouldReturnCreated() throws Exception {
        TestDriveRequestDTO request = TestDriveRequestDTO.builder()
                .carId(1L)
                .customerName("Bruce Wayne")
                .phone("+91 9999999999")
                .email("bruce@waynecorp.com")
                .preferredDate(LocalDate.now().plusDays(3))
                .timeSlot("14:00 - 16:00 VIP Slot")
                .experienceType(ExperienceType.TRACK)
                .build();

        TestDriveResponseDTO response = TestDriveResponseDTO.builder()
                .id(1L)
                .carId(1L)
                .carName("Rolls-Royce Phantom")
                .carBrand("Rolls-Royce")
                .customerName("Bruce Wayne")
                .referenceCode("TD-2026-WAYNE01")
                .status(TestDriveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(testDriveService.createTestDrive(any(TestDriveRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/test-drives")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceCode").value("TD-2026-WAYNE01"))
                .andExpect(jsonPath("$.customerName").value("Bruce Wayne"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/test-drives/ref/{referenceCode} - should return booking details")
    void getByReferenceCode_shouldReturnDetails() throws Exception {
        TestDriveResponseDTO response = TestDriveResponseDTO.builder()
                .id(1L)
                .referenceCode("TD-2026-WAYNE01")
                .customerName("Bruce Wayne")
                .status(TestDriveStatus.PENDING)
                .build();

        when(testDriveService.getByReferenceCode("TD-2026-WAYNE01")).thenReturn(response);

        mockMvc.perform(get("/api/test-drives/ref/TD-2026-WAYNE01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode").value("TD-2026-WAYNE01"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/test-drives/{referenceCode}/tracking - should return Amazon-style tracking timeline")
    void getTrackingTimeline_shouldReturnTimeline() throws Exception {
        TestDriveTrackingDTO tracking = TestDriveTrackingDTO.builder()
                .referenceCode("TD-2026-WAYNE01")
                .carName("Pagani Huayra R")
                .currentStep(2)
                .currentStatus(TestDriveStatus.CONCIERGE_ASSIGNED)
                .steps(List.of(
                        TestDriveTrackingDTO.TrackingStep.builder()
                                .stepNumber(1)
                                .title("VIP Reservation Received")
                                .completed(true)
                                .build(),
                        TestDriveTrackingDTO.TrackingStep.builder()
                                .stepNumber(2)
                                .title("Master Concierge Assigned")
                                .completed(true)
                                .build()
                ))
                .build();

        when(testDriveService.getTrackingTimeline("TD-2026-WAYNE01")).thenReturn(tracking);

        mockMvc.perform(get("/api/test-drives/TD-2026-WAYNE01/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode").value("TD-2026-WAYNE01"))
                .andExpect(jsonPath("$.currentStep").value(2))
                .andExpect(jsonPath("$.steps[0].title").value("VIP Reservation Received"));
    }

    @Test
    @DisplayName("PUT /api/test-drives/{id}/status - should update status when authenticated as ADMIN")
    void updateStatus_shouldReturnUpdated() throws Exception {
        TestDriveResponseDTO response = TestDriveResponseDTO.builder()
                .id(1L)
                .status(TestDriveStatus.CONFIRMED)
                .build();

        when(testDriveService.updateStatus(eq(1L), eq(TestDriveStatus.CONFIRMED))).thenReturn(response);

        mockMvc.perform(put("/api/test-drives/1/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
