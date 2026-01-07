package com.ticketblitz.booking.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8083}")
    private String inventoryServiceUrl;

    /**
     * Verify that all seats are locked by the specified user
     */
    public boolean verifySeatsLocked(Long eventId, List<String> seatIds, String userId) {
        log.info("Verifying seats locked: eventId={}, seatIds={}, userId={}", 
                eventId, seatIds, userId);

        try {
            for (String seatId : seatIds) {
                String url = String.format("%s/inventory/check/%d/%s", 
                        inventoryServiceUrl, eventId, seatId);
                
                SeatLockStatus status = restTemplate.getForObject(url, SeatLockStatus.class);
                
                if (status == null || !status.isLocked()) {
                    log.warn("Seat not locked: eventId={}, seatId={}", eventId, seatId);
                    return false;
                }
                
                if (!userId.equals(status.getOwner())) {
                    log.warn("Seat locked by different user: eventId={}, seatId={}, owner={}, expected={}", 
                            eventId, seatId, status.getOwner(), userId);
                    return false;
                }
            }
            
            log.info("All seats verified: eventId={}, seatIds={}", eventId, seatIds);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to verify seats: eventId={}, seatIds={}", eventId, seatIds, e);
            return false;
        }
    }

    /**
     * Release seat locks (called when booking is cancelled or expired)
     */
    public void releaseSeats(Long eventId, List<String> seatIds, String userId) {
        log.info("Releasing seats: eventId={}, seatIds={}, userId={}", eventId, seatIds, userId);

        try {
            for (String seatId : seatIds) {
                String url = String.format("%s/inventory/release", inventoryServiceUrl);
                
                ReleaseRequest request = new ReleaseRequest(eventId, seatId, userId);
                restTemplate.postForObject(url, request, Void.class);
            }
            
            log.info("Seats released: eventId={}, seatIds={}", eventId, seatIds);
            
        } catch (Exception e) {
            log.error("Failed to release seats: eventId={}, seatIds={}", eventId, seatIds, e);
        }
    }

    // DTOs for communication with Inventory Service
    
    public static class SeatLockStatus {
        private Long eventId;
        private String seatId;
        private boolean locked;
        private String owner;
        private Long ttlSeconds;

        public SeatLockStatus() {}

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }

        public String getSeatId() { return seatId; }
        public void setSeatId(String seatId) { this.seatId = seatId; }

        public boolean isLocked() { return locked; }
        public void setLocked(boolean locked) { this.locked = locked; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public Long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    public static class ReleaseRequest {
        private Long eventId;
        private String seatId;
        private String userId;

        public ReleaseRequest() {}

        public ReleaseRequest(Long eventId, String seatId, String userId) {
            this.eventId = eventId;
            this.seatId = seatId;
            this.userId = userId;
        }

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }

        public String getSeatId() { return seatId; }
        public void setSeatId(String seatId) { this.seatId = seatId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
