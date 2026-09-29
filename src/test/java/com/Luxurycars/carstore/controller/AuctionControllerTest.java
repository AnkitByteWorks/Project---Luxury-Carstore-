package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.config.CacheConfig;
import com.Luxurycars.carstore.config.JwtAuthFilter;
import com.Luxurycars.carstore.config.RateLimitFilter;
import com.Luxurycars.carstore.config.SecurityConfig;
import com.Luxurycars.carstore.dto.AuctionLotResponseDTO;
import com.Luxurycars.carstore.dto.BidRequestDTO;
import com.Luxurycars.carstore.dto.BidResponseDTO;
import com.Luxurycars.carstore.service.AuctionService;
import com.Luxurycars.carstore.service.CustomUserDetailsService;
import com.Luxurycars.carstore.service.JwtService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuctionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitFilter.class
        )
)
@Import({SecurityConfig.class, JwtAuthFilter.class, CacheConfig.class})
@ActiveProfiles("test")
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/auctions/active - should return all active lots with countdown")
    void getActiveLots_shouldReturnActiveLots() throws Exception {
        AuctionLotResponseDTO lotDTO = AuctionLotResponseDTO.builder()
                .id(1L)
                .title("2024 Pagani Huayra R")
                .startingBid(new BigDecimal("280000000.00"))
                .currentBid(new BigDecimal("290000000.00"))
                .minIncrement(new BigDecimal("500000.00"))
                .reserveMet(false)
                .active(true)
                .countdownSeconds(3600L)
                .countdown("01h 00m 00s")
                .totalBids(5)
                .build();

        when(auctionService.getActiveLots()).thenReturn(List.of(lotDTO));

        mockMvc.perform(get("/api/auctions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("2024 Pagani Huayra R"))
                .andExpect(jsonPath("$[0].currentBid").value(290000000.00))
                .andExpect(jsonPath("$[0].countdown").value("01h 00m 00s"));
    }

    @Test
    @DisplayName("POST /api/auctions/{lotId}/bid - should accept valid bid and return high bid update")
    void placeBid_shouldReturnUpdatedBid() throws Exception {
        BidResponseDTO bidResponse = BidResponseDTO.builder()
                .id(101L)
                .lotId(1L)
                .bidderName("Collector X")
                .bidderLocation("Monaco")
                .amount(new BigDecimal("295000000.00"))
                .bidPlacedAt(LocalDateTime.now())
                .currentBid(new BigDecimal("295000000.00"))
                .reserveMet(true)
                .minNextBid(new BigDecimal("295500000.00"))
                .build();

        when(auctionService.placeBid(eq(1L), any(BidRequestDTO.class))).thenReturn(bidResponse);

        BidRequestDTO request = BidRequestDTO.builder()
                .bidderName("Collector X")
                .bidderLocation("Monaco")
                .amount(new BigDecimal("295000000.00"))
                .build();

        mockMvc.perform(post("/api/auctions/1/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.lotId").value(1))
                .andExpect(jsonPath("$.bidderName").value("Collector X"))
                .andExpect(jsonPath("$.amount").value(295000000.00))
                .andExpect(jsonPath("$.currentBid").value(295000000.00))
                .andExpect(jsonPath("$.reserveMet").value(true));
    }

    @Test
    @DisplayName("GET /api/auctions/{lotId}/stream - should establish SSE stream")
    void streamLotBids_shouldReturnSseStream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(auctionService.subscribeToLot(1L)).thenReturn(emitter);

        mockMvc.perform(get("/api/auctions/1/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}
