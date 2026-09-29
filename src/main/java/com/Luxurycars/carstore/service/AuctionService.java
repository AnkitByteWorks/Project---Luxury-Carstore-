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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionLotRepository auctionLotRepository;
    private final BidRepository bidRepository;

    // SSE subscribers per lotId
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> lotEmitters = new ConcurrentHashMap<>();

    @Autowired
    public AuctionService(AuctionLotRepository auctionLotRepository, BidRepository bidRepository) {
        this.auctionLotRepository = auctionLotRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Retrieve all active auction lots with high bid and countdown metrics.
     */
    public List<AuctionLotResponseDTO> getActiveLots() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionLot> lots = auctionLotRepository.findByActiveTrueOrderByLotEndsAtAsc();

        return lots.stream()
                .map(lot -> {
                    // Check if auction expired
                    if (lot.getLotEndsAt() != null && lot.getLotEndsAt().isBefore(now)) {
                        lot.setActive(false);
                        auctionLotRepository.save(lot);
                    }
                    return toLotResponseDTO(lot, now);
                })
                .filter(AuctionLotResponseDTO::getActive)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single auction lot by ID.
     */
    public AuctionLotResponseDTO getLotById(Long lotId) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction lot not found with id: " + lotId));
        return toLotResponseDTO(lot, LocalDateTime.now());
    }

    /**
     * Place a live bid on an active auction lot.
     * Validates that the submitted bid is strictly greater than currentBid + minIncrement (e.g. +5,00,000 INR),
     * updates the high bid, logs the bid, and broadcasts via SSE.
     */
    @Transactional
    public synchronized BidResponseDTO placeBid(Long lotId, BidRequestDTO request) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction lot not found with id: " + lotId));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.FALSE.equals(lot.getActive()) || (lot.getLotEndsAt() != null && lot.getLotEndsAt().isBefore(now))) {
            lot.setActive(false);
            auctionLotRepository.save(lot);
            throw new BadRequestException("Auction lot #" + lotId + " has closed or is inactive.");
        }

        BigDecimal currentBid = lot.getCurrentBid() != null ? lot.getCurrentBid() : lot.getStartingBid();
        BigDecimal minIncrement = lot.getMinIncrement() != null ? lot.getMinIncrement() : new BigDecimal("500000.00");
        BigDecimal minimumRequired = currentBid.add(minIncrement);

        if (request.getAmount() == null || request.getAmount().compareTo(minimumRequired) <= 0) {
            throw new BadRequestException("Submitted bid (" + request.getAmount()
                    + ") must be strictly greater than current bid + min increment (" + minimumRequired + " INR).");
        }

        // Update high bid on the lot
        lot.setCurrentBid(request.getAmount());
        if (lot.getReservePrice() != null && request.getAmount().compareTo(lot.getReservePrice()) >= 0) {
            lot.setReserveMet(true);
        }
        AuctionLot updatedLot = auctionLotRepository.save(lot);

        // Record bid entry
        String bidderName = (request.getBidderName() != null && !request.getBidderName().isBlank())
                ? request.getBidderName().trim()
                : "VIP Collector";
        String bidderLocation = (request.getBidderLocation() != null && !request.getBidderLocation().isBlank())
                ? request.getBidderLocation().trim()
                : "Global Desk";

        Bid bid = Bid.builder()
                .lotId(lotId)
                .bidderName(bidderName)
                .bidderLocation(bidderLocation)
                .amount(request.getAmount())
                .bidPlacedAt(now)
                .build();
        Bid savedBid = bidRepository.save(bid);

        log.info("🏆 New high bid placed on Lot #{}: ₹{} by {} ({})",
                lotId, request.getAmount(), bidderName, bidderLocation);

        BigDecimal nextMinBid = updatedLot.getCurrentBid().add(minIncrement);

        BidResponseDTO response = BidResponseDTO.builder()
                .id(savedBid.getId())
                .lotId(lotId)
                .bidderName(savedBid.getBidderName())
                .bidderLocation(savedBid.getBidderLocation())
                .amount(savedBid.getAmount())
                .bidPlacedAt(savedBid.getBidPlacedAt())
                .currentBid(updatedLot.getCurrentBid())
                .reserveMet(updatedLot.getReserveMet())
                .minNextBid(nextMinBid)
                .build();

        // Broadcast event in real-time to all connected collectors watching this lot
        broadcastBidEvent(lotId, response);

        return response;
    }

    /**
     * Subscribe to live SSE events for a specific auction lot.
     */
    public SseEmitter subscribeToLot(Long lotId) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction lot not found with id: " + lotId));

        log.info("Collector subscribed to live auction stream for lot ID: {}", lotId);

        // 30 minute timeout
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        lotEmitters.computeIfAbsent(lotId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(lotId, emitter));
        emitter.onTimeout(() -> removeEmitter(lotId, emitter));
        emitter.onError(e -> removeEmitter(lotId, emitter));

        // Initial handshake event with current lot details and recent bids
        try {
            List<Bid> recentBids = bidRepository.findByLotIdOrderByBidPlacedAtDesc(lotId);
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of(
                            "lotId", lotId,
                            "lot", toLotResponseDTO(lot, LocalDateTime.now()),
                            "recentBids", recentBids.stream().limit(10).collect(Collectors.toList()),
                            "message", "Connected to live auction room for lot #" + lotId,
                            "timestamp", LocalDateTime.now().toString()
                    )));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE handshake for lot {}: {}", lotId, e.getMessage());
            removeEmitter(lotId, emitter);
        }

        return emitter;
    }

    /**
     * Broadcast new bid to all SSE clients subscribed to the lot.
     */
    public void broadcastBidEvent(Long lotId, BidResponseDTO bid) {
        List<SseEmitter> emitters = lotEmitters.get(lotId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE listeners for auction lot {}", lotId);
            return;
        }

        log.info("Broadcasting new bid event for lot ID: {} to {} subscriber(s)", lotId, emitters.size());
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("BID_PLACED")
                        .data(bid));
            } catch (Exception e) {
                log.warn("Error sending bid SSE to subscriber for lot {}: {}", lotId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
        if (emitters.isEmpty()) {
            lotEmitters.remove(lotId);
        }
    }

    /**
     * Periodic 25-second heartbeat ping to prevent connection timeout.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (lotEmitters.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : lotEmitters.entrySet()) {
            Long lotId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("HEARTBEAT")
                            .data("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                lotEmitters.remove(lotId);
            }
        }
    }

    /**
     * Get bid history for a lot.
     */
    public List<BidResponseDTO> getBidsForLot(Long lotId) {
        AuctionLot lot = auctionLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction lot not found with id: " + lotId));
        BigDecimal minIncrement = lot.getMinIncrement() != null ? lot.getMinIncrement() : new BigDecimal("500000.00");

        return bidRepository.findByLotIdOrderByBidPlacedAtDesc(lotId).stream()
                .map(b -> BidResponseDTO.builder()
                        .id(b.getId())
                        .lotId(b.getLotId())
                        .bidderName(b.getBidderName())
                        .bidderLocation(b.getBidderLocation())
                        .amount(b.getAmount())
                        .bidPlacedAt(b.getBidPlacedAt())
                        .currentBid(lot.getCurrentBid())
                        .reserveMet(lot.getReserveMet())
                        .minNextBid(lot.getCurrentBid().add(minIncrement))
                        .build())
                .collect(Collectors.toList());
    }

    private void removeEmitter(Long lotId, SseEmitter emitter) {
        List<SseEmitter> emitters = lotEmitters.get(lotId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                lotEmitters.remove(lotId);
            }
        }
        log.debug("Removed SSE emitter for lot {}", lotId);
    }

    private AuctionLotResponseDTO toLotResponseDTO(AuctionLot lot, LocalDateTime now) {
        long secondsRemaining = 0;
        String countdown = "CLOSED";

        if (lot.getLotEndsAt() != null && lot.getLotEndsAt().isAfter(now)) {
            Duration duration = Duration.between(now, lot.getLotEndsAt());
            secondsRemaining = Math.max(0, duration.getSeconds());
            long days = duration.toDays();
            long hours = duration.toHoursPart();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            if (days > 0) {
                countdown = String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
            } else {
                countdown = String.format("%02dh %02dm %02ds", hours, minutes, seconds);
            }
        }

        BigDecimal minIncrement = lot.getMinIncrement() != null ? lot.getMinIncrement() : new BigDecimal("500000.00");
        BigDecimal current = lot.getCurrentBid() != null ? lot.getCurrentBid() : lot.getStartingBid();
        BigDecimal nextMin = current.add(minIncrement);

        List<Bid> bids = bidRepository.findByLotIdOrderByBidPlacedAtDesc(lot.getId());

        return AuctionLotResponseDTO.builder()
                .id(lot.getId())
                .title(lot.getTitle())
                .description(lot.getDescription())
                .imageUrl(lot.getImageUrl())
                .startingBid(lot.getStartingBid())
                .currentBid(current)
                .minIncrement(minIncrement)
                .reserveMet(lot.getReserveMet())
                .active(lot.getActive() && secondsRemaining > 0)
                .lotEndsAt(lot.getLotEndsAt())
                .countdownSeconds(secondsRemaining)
                .countdown(countdown)
                .totalBids(bids.size())
                .minNextBid(nextMin)
                .build();
    }

    public Map<Long, CopyOnWriteArrayList<SseEmitter>> getLotEmitters() {
        return lotEmitters;
    }
}
