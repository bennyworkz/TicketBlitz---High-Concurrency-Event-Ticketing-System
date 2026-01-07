package com.ticketblitz.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event published when booking is confirmed
 * Consumed by: Notification Service, Analytics Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
    
    private Long bookingId;
    
    private String userId;
    
    private Long eventId;
    
    private String eventName;
    
    private String eventDate;
    
    private String email;
    
    private List<String> seatIds;
    
    private Instant timestamp;
    
    private String eventType = "BOOKING_CONFIRMED";
}
