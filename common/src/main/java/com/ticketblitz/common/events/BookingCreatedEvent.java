package com.ticketblitz.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Event published when a booking is created (PENDING status)
 * Consumed by: Payment Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    
    private Long bookingId;
    
    private String userId;
    
    private Long eventId;
    
    private List<String> seatIds;
    
    private BigDecimal amount;
    
    private Instant timestamp;
    
    private String eventType = "BOOKING_CREATED";
}
