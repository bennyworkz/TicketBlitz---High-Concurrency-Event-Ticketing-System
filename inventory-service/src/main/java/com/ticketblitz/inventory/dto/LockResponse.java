package com.ticketblitz.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lock Response DTO
 * Response for lock/release operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockResponse {

    private boolean success;
    private String message;
    private Long eventId;
    private String seatId;
    private List<String> seatIds;
    private String userId;
    private String lockedBy;  // Who currently holds the lock
    private Long ttlSeconds;  // Remaining lock time
}
