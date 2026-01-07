package com.ticketblitz.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when payment fails
 * Consumed by: Booking Service (to cancel booking), Inventory Service (to release lock)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    
    private Long bookingId;
    
    private String userId;
    
    private String email;
    
    private String reason;
    
    private Instant timestamp;
    
    private String eventType = "PAYMENT_FAILED";
}
