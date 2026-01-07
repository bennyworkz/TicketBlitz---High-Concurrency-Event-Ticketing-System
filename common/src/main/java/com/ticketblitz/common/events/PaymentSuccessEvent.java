package com.ticketblitz.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when payment is successful
 * Consumed by: Booking Service, Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    
    private Long bookingId;
    
    private Long transactionId;
    
    private String userId;
    
    private String email;
    
    private BigDecimal amount;
    
    private String gatewayReference;
    
    private Instant timestamp;
    
    private String eventType = "PAYMENT_SUCCESS";
}
