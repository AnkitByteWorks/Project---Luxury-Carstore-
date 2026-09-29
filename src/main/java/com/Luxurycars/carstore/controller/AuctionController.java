package com.Luxurycars.carstore.controller;

import com.Luxurycars.carstore.dto.AuctionLotResponseDTO;
import com.Luxurycars.carstore.dto.BidRequestDTO;
import com.Luxurycars.carstore.dto.BidResponseDTO;
import com.Luxurycars.carstore.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Live Hypercar Auctions", description = "Real-time SSE and REST API for exclusive hypercar auction rooms")
public class AuctionController {

    private final AuctionService auctionService;

    @Autowired
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Operation(summary = "Get all active hypercar auction lots with high bid and live countdown")
    @GetMapping("/active")
    public ResponseEntity<List<AuctionLotResponseDTO>> getActiveLots() {
        return ResponseEntity.ok(auctionService.getActiveLots());
    }

    @Operation(summary = "Get specific auction lot details by ID")
    @GetMapping("/{lotId}")
    public ResponseEntity<AuctionLotResponseDTO> getLotById(@PathVariable Long lotId) {
        return ResponseEntity.ok(auctionService.getLotById(lotId));
    }

    @Operation(summary = "Place a live bid on an auction lot")
    @PostMapping("/{lotId}/bid")
    public ResponseEntity<BidResponseDTO> placeBid(
            @PathVariable Long lotId,
            @Valid @RequestBody BidRequestDTO request) {
        return ResponseEntity.ok(auctionService.placeBid(lotId, request));
    }

    @Operation(summary = "SSE stream for real-time live bidding updates in the auction room")
    @GetMapping(value = "/{lotId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLotBids(@PathVariable Long lotId) {
        return auctionService.subscribeToLot(lotId);
    }

    @Operation(summary = "Get bid log history for a specific auction lot")
    @GetMapping("/{lotId}/bids")
    public ResponseEntity<List<BidResponseDTO>> getLotBids(@PathVariable Long lotId) {
        return ResponseEntity.ok(auctionService.getBidsForLot(lotId));
    }
}
