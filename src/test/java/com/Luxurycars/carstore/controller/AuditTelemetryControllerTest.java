package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.RateLimitFilter;
import com.Luxurycars.carstore.mongodb.document.CustomerAuditLog;
import com.Luxurycars.carstore.mongodb.document.VehicleTelemetryLog;
import com.Luxurycars.carstore.mongodb.repository.CustomerAuditLogRepository;
import com.Luxurycars.carstore.mongodb.repository.VehicleTelemetryLogRepository;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuditTelemetryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@org.springframework.context.annotation.Import({com.Luxurycars.carstore.config.SecurityConfig.class, com.Luxurycars.carstore.config.JwtAuthFilter.class, com.Luxurycars.carstore.config.CacheConfig.class})
@ActiveProfiles("test")
class AuditTelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private CustomerAuditLogRepository customerAuditLogRepository;

    @MockitoBean
    private VehicleTelemetryLogRepository vehicleTelemetryLogRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/analytics/interaction - should record customer 3D event in MongoDB")
    void recordInteraction_shouldReturnCreated() throws Exception {
        CustomerAuditLog saved = CustomerAuditLog.builder()
                .id("mongo-log-123")
                .customerIdentifier("collector@luxury.com")
                .customerName("Lord Sterling")
                .actionType("3D_DOOR_OPEN")
                .carModel("Pagani Huayra R")
                .timestamp(LocalDateTime.now())
                .build();

        when(customerAuditLogRepository.save(any(CustomerAuditLog.class))).thenReturn(saved);

        Map<String, Object> payload = Map.of(
                "customerId", "collector@luxury.com",
                "customerName", "Lord Sterling",
                "actionType", "3D_DOOR_OPEN",
                "carModel", "Pagani Huayra R",
                "metadata", Map.of("angle", 45, "component", "Gullwing Door")
        );

        mockMvc.perform(post("/api/analytics/interaction")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.mongoId").value("mongo-log-123"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/analytics/customer-audit - should return recent audit logs")
    void getRecentAuditLogs_shouldReturnList() throws Exception {
        CustomerAuditLog logEntry = CustomerAuditLog.builder()
                .id("log-1")
                .customerName("Bruce Wayne")
                .actionType("TEST_DRIVE_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();

        when(customerAuditLogRepository.findTop20ByOrderByTimestampDesc()).thenReturn(List.of(logEntry));

        mockMvc.perform(get("/api/analytics/customer-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("log-1"))
                .andExpect(jsonPath("$[0].customerName").value("Bruce Wayne"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/analytics/telemetry/{orderId} - should return vehicle telemetry")
    void getVehicleTelemetry_shouldReturnTelemetry() throws Exception {
        VehicleTelemetryLog telemetry = VehicleTelemetryLog.builder()
                .id("telem-1")
                .orderId(101L)
                .speedKmH(140.5)
                .gForce(1.2)
                .recordedAt(LocalDateTime.now())
                .build();

        when(vehicleTelemetryLogRepository.findByOrderIdOrderByRecordedAtDesc(101L)).thenReturn(List.of(telemetry));

        mockMvc.perform(get("/api/analytics/telemetry/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(101))
                .andExpect(jsonPath("$[0].speedKmH").value(140.5));
    }
}
