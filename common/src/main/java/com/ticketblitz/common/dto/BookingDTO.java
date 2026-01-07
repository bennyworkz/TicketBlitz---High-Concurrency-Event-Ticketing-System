package com.ticketblitz.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Booking Data Transfer Object
 * Used across multiple services (Booking, Payment, Notification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    
    private Long id;
    
    private String userId;
    
    private Long eventId;
    
    private String eventName;
    
    private List<String> seatIds;
    
    private BigDecimal amount;
    
    private String status; // PENDING, CONFIRMED, CANCELLED
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant confirmedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;
}
