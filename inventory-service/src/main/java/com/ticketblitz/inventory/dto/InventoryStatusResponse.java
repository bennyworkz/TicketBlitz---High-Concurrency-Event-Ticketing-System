package com.ticketblitz.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Inventory Status Response DTO
 * Shows current inventory status for an event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatusResponse {

    private Long eventId;
    
    // VISUAL mode status
    private int lockedSeatsCount;
    private Set<String> lockedSeats;
    
    // TATKAL mode status
    private int tatkalRemainingSeats;
    private boolean tatkalSoldOut;
}
