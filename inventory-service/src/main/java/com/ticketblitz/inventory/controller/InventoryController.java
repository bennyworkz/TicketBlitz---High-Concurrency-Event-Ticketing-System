package com.ticketblitz.inventory.controller;

import com.ticketblitz.inventory.dto.LockRequest;
import com.ticketblitz.inventory.dto.LockResponse;
import com.ticketblitz.inventory.dto.InventoryStatusResponse;
import com.ticketblitz.inventory.service.SeatLockService;
import com.ticketblitz.inventory.service.TatkalInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Inventory Controller
 * REST API for seat locking and inventory management
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "Distributed seat locking and Tatkal inventory management")
public class InventoryController {

    private final SeatLockService seatLockService;
    private final TatkalInventoryService tatkalInventoryService;

    /**
     * Lock a seat (VISUAL mode)
     * POST /inventory/lock
     */
    @PostMapping("/lock")
    @Operation(summary = "Lock a seat", description = "Locks a seat for a user with Redis distributed lock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lock operation completed",
            content = @Content(schema = @Schema(implementation = LockResponse.class)))
    })
    public ResponseEntity<LockResponse> lockSeat(@Valid @RequestBody LockRequest request) {
        log.info("Lock seat request: event={}, seat={}, user={}", 
                request.getEventId(), request.getSeatId(), request.getUserId());
        
        boolean locked = seatLockService.tryLockSeat(
                request.getEventId(), 
                request.getSeatId(), 
                request.getUserId()
        );
        
        if (locked) {
            long ttl = seatLockService.getSeatLockTTL(request.getEventId(), request.getSeatId());
            return ResponseEntity.ok(LockResponse.builder()
                    .success(true)
                    .message("Seat locked successfully")
                    .eventId(request.getEventId())
                    .seatId(request.getSeatId())
                    .userId(request.getUserId())
                    .ttlSeconds(ttl)
                    .build());
        } else {
            String owner = seatLockService.getSeatLockOwner(request.getEventId(), request.getSeatId());
            return ResponseEntity.ok(LockResponse.builder()
                    .success(false)
                    .message("Seat already locked by another user")
                    .eventId(request.getEventId())
                    .seatId(request.getSeatId())
                    .lockedBy(owner)
                    .build());
        }
    }

    /**
     * Lock multiple seats (VISUAL mode)
     * POST /inventory/lock-multiple
     */
    @PostMapping("/lock-multiple")
    public ResponseEntity<LockResponse> lockMultipleSeats(@Valid @RequestBody LockRequest request) {
        log.info("Lock multiple seats request: event={}, seats={}, user={}", 
                request.getEventId(), request.getSeatIds(), request.getUserId());
        
        boolean locked = seatLockService.tryLockSeats(
                request.getEventId(), 
                request.getSeatIds(), 
                request.getUserId()
        );
        
        if (locked) {
            return ResponseEntity.ok(LockResponse.builder()
                    .success(true)
                    .message("All seats locked successfully")
                    .eventId(request.getEventId())
                    .seatIds(request.getSeatIds())
                    .userId(request.getUserId())
                    .build());
        } else {
            return ResponseEntity.ok(LockResponse.builder()
                    .success(false)
                    .message("Failed to lock all seats")
                    .eventId(request.getEventId())
                    .seatIds(request.getSeatIds())
                    .build());
        }
    }

    /**
     * Release a seat lock (VISUAL mode)
     * POST /inventory/release
     */
    @PostMapping("/release")
    public ResponseEntity<LockResponse> releaseSeat(@Valid @RequestBody LockRequest request) {
        log.info("Release seat request: event={}, seat={}, user={}", 
                request.getEventId(), request.getSeatId(), request.getUserId());
        
        boolean released = seatLockService.releaseSeat(
                request.getEventId(), 
                request.getSeatId(), 
                request.getUserId()
        );
        
        return ResponseEntity.ok(LockResponse.builder()
                .success(released)
                .message(released ? "Seat released successfully" : "Failed to release seat")
                .eventId(request.getEventId())
                .seatId(request.getSeatId())
                .userId(request.getUserId())
                .build());
    }

    /**
     * Reserve a Tatkal seat
     * POST /inventory/tatkal/reserve/{eventId}
     */
    @PostMapping("/tatkal/reserve/{eventId}")
    public ResponseEntity<Map<String, Object>> reserveTatkalSeat(@PathVariable Long eventId) {
        log.info("Tatkal reserve request: event={}", eventId);
        
        boolean reserved = tatkalInventoryService.tryReserveSeat(eventId);
        int remaining = tatkalInventoryService.getRemainingInventory(eventId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", reserved);
        response.put("message", reserved ? "Seat reserved" : "Sold out");
        response.put("eventId", eventId);
        response.put("remainingSeats", remaining);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Release a Tatkal seat
     * POST /inventory/tatkal/release/{eventId}
     */
    @PostMapping("/tatkal/release/{eventId}")
    public ResponseEntity<Map<String, Object>> releaseTatkalSeat(@PathVariable Long eventId) {
        log.info("Tatkal release request: event={}", eventId);
        
        tatkalInventoryService.releaseSeat(eventId);
        int remaining = tatkalInventoryService.getRemainingInventory(eventId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Seat released");
        response.put("eventId", eventId);
        response.put("remainingSeats", remaining);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get inventory status
     * GET /inventory/status/{eventId}
     */
    @GetMapping("/status/{eventId}")
    public ResponseEntity<InventoryStatusResponse> getInventoryStatus(@PathVariable Long eventId) {
        log.info("Inventory status request: event={}", eventId);
        
        // Get locked seats (VISUAL mode)
        Set<String> lockedSeats = seatLockService.getLockedSeats(eventId);
        
        // Get Tatkal inventory
        int tatkalRemaining = tatkalInventoryService.getRemainingInventory(eventId);
        boolean tatkalSoldOut = tatkalInventoryService.isSoldOut(eventId);
        
        return ResponseEntity.ok(InventoryStatusResponse.builder()
                .eventId(eventId)
                .lockedSeatsCount(lockedSeats.size())
                .lockedSeats(lockedSeats)
                .tatkalRemainingSeats(tatkalRemaining)
                .tatkalSoldOut(tatkalSoldOut)
                .build());
    }

    /**
     * Check if seat is locked
     * GET /inventory/check/{eventId}/{seatId}
     */
    @GetMapping("/check/{eventId}/{seatId}")
    public ResponseEntity<Map<String, Object>> checkSeatLock(
            @PathVariable Long eventId,
            @PathVariable String seatId) {
        
        boolean locked = seatLockService.isSeatLocked(eventId, seatId);
        String owner = seatLockService.getSeatLockOwner(eventId, seatId);
        long ttl = seatLockService.getSeatLockTTL(eventId, seatId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", eventId);
        response.put("seatId", seatId);
        response.put("locked", locked);
        response.put("owner", owner);
        response.put("ttlSeconds", ttl);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Initialize Tatkal inventory (admin)
     * POST /inventory/tatkal/init/{eventId}
     */
    @PostMapping("/tatkal/init/{eventId}")
    public ResponseEntity<Map<String, Object>> initializeTatkalInventory(
            @PathVariable Long eventId,
            @RequestParam int totalSeats) {
        
        log.info("Initialize Tatkal inventory: event={}, seats={}", eventId, totalSeats);
        
        tatkalInventoryService.initializeInventory(eventId, totalSeats);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inventory initialized");
        response.put("eventId", eventId);
        response.put("totalSeats", totalSeats);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     * GET /inventory/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "inventory-service");
        return ResponseEntity.ok(response);
    }
}
