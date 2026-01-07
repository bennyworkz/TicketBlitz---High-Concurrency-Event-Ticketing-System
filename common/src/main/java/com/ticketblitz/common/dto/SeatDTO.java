package com.ticketblitz.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Seat Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    
    private Long id;
    
    private Long eventId;
    
    private String rowNumber;
    
    private String seatNumber;
    
    private BigDecimal price;
    
    private String status; // AVAILABLE, LOCKED, BOOKED
    
    private String lockedBy; // User ID who locked the seat
}
