package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.AuctionLotResponseDTO;
import com.Luxurycars.carstore.dto.BidRequestDTO;
import com.Luxurycars.carstore.dto.BidResponseDTO;
import com.Luxurycars.carstore.entity.AuctionLot;
import com.Luxurycars.carstore.entity.Bid;
import com.Luxurycars.carstore.exception.BadRequestException;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.AuctionLotRepository;
import com.Luxurycars.carstore.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionLotRepository auctionLotRepository;

    @Mock
    private BidRepository bidRepository;

    private AuctionService auctionService;

    private AuctionLot testLot;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(auctionLotRepository, bidRepository);

        testLot = AuctionLot.builder()
                .id(1L)
                .title("2024 Pagani Huayra R")
                .description("V12 track monster")
                .imageUrl("https://example.com/huayra.jpg")
                .startingBid(new BigDecimal("280000000.00"))
                .currentBid(new BigDecimal("290000000.00"))
                .reservePrice(new BigDecimal("300000000.00"))
                .minIncrement(new BigDecimal("500000.00"))
                .reserveMet(false)
                .active(true)
                .lotEndsAt(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Test
    @DisplayName("getActiveLots - should return currently active lots with countdown")
    void getActiveLots_shouldReturnActiveLots() {
        when(auctionLotRepository.findByActiveTrueOrderByLotEndsAtAsc()).thenReturn(List.of(testLot));
        when(bidRepository.findByLotIdOrderByBidPlacedAtDesc(1L)).thenReturn(List.of());

        List<AuctionLotResponseDTO> lots = auctionService.getActiveLots();

        assertThat(lots).hasSize(1);
        assertThat(lots.get(0).getId()).isEqualTo(1L);
        assertThat(lots.get(0).getTitle()).isEqualTo("2024 Pagani Huayra R");
        assertThat(lots.get(0).getCurrentBid()).isEqualByComparingTo("290000000.00");
        assertThat(lots.get(0).getMinIncrement()).isEqualByComparingTo("500000.00");
        assertThat(lots.get(0).getCountdownSeconds()).isGreaterThan(0L);
        assertThat(lots.get(0).getCountdown()).isNotNull();
    }

    @Test
    @DisplayName("placeBid - should place valid bid strictly greater than currentBid + minIncrement")
    void placeBid_shouldSucceedWhenBidStrictlyGreaterThanThreshold() {
        when(auctionLotRepository.findById(1L)).thenReturn(Optional.of(testLot));
        when(auctionLotRepository.save(any(AuctionLot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid b = inv.getArgument(0);
            b.setId(501L);
            return b;
        });

        // Current bid is 290,000,000. Min increment is 500,000. Threshold is 290,500,000.
        // Bidder submits 300,000,000 (also meets reserve 300,000,000)
        BidRequestDTO request = BidRequestDTO.builder()
                .bidderName("Collector Sheikh")
                .bidderLocation("Dubai")
                .amount(new BigDecimal("300000000.00"))
                .build();

        BidResponseDTO response = auctionService.placeBid(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("300000000.00");
        assertThat(response.getCurrentBid()).isEqualByComparingTo("300000000.00");
        assertThat(response.getReserveMet()).isTrue();
        assertThat(response.getBidderName()).isEqualTo("Collector Sheikh");
        assertThat(response.getBidderLocation()).isEqualTo("Dubai");

        verify(auctionLotRepository, times(1)).save(testLot);
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    @DisplayName("placeBid - should reject bid equal to currentBid + minIncrement (must be strictly greater)")
    void placeBid_shouldThrowWhenBidNotStrictlyGreater() {
        when(auctionLotRepository.findById(1L)).thenReturn(Optional.of(testLot));

        // Threshold is exactly 290,500,000. Submitting 290,500,000 should fail strictly-greater validation
        BidRequestDTO request = BidRequestDTO.builder()
                .bidderName("Bargain Hunter")
                .amount(new BigDecimal("290500000.00"))
                .build();

        assertThatThrownBy(() -> auctionService.placeBid(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("strictly greater than");

        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    @DisplayName("placeBid - should throw ResourceNotFoundException when lot does not exist")
    void placeBid_shouldThrowWhenLotNotFound() {
        when(auctionLotRepository.findById(999L)).thenReturn(Optional.empty());

        BidRequestDTO request = BidRequestDTO.builder()
                .amount(new BigDecimal("350000000.00"))
                .build();

        assertThatThrownBy(() -> auctionService.placeBid(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction lot not found");
    }

    @Test
    @DisplayName("placeBid - should throw BadRequestException when lot has ended")
    void placeBid_shouldThrowWhenLotHasEnded() {
        testLot.setLotEndsAt(LocalDateTime.now().minusMinutes(5));
        when(auctionLotRepository.findById(1L)).thenReturn(Optional.of(testLot));

        BidRequestDTO request = BidRequestDTO.builder()
                .amount(new BigDecimal("350000000.00"))
                .build();

        assertThatThrownBy(() -> auctionService.placeBid(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("has closed or is inactive");
    }

    @Test
    @DisplayName("subscribeToLot - should establish SSE emitter with initial handshake")
    void subscribeToLot_shouldEstablishSseEmitter() {
        when(auctionLotRepository.findById(1L)).thenReturn(Optional.of(testLot));
        when(bidRepository.findByLotIdOrderByBidPlacedAtDesc(1L)).thenReturn(List.of());

        SseEmitter emitter = auctionService.subscribeToLot(1L);

        assertThat(emitter).isNotNull();
        assertThat(auctionService.getLotEmitters().get(1L)).isNotNull();
        assertThat(auctionService.getLotEmitters().get(1L)).contains(emitter);
    }
}
