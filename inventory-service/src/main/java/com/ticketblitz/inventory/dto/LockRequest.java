package com.ticketblitz.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lock Request DTO
 * Used for locking/releasing seats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    private String seatId;  // For single seat operations

    private List<String> seatIds;  // For multiple seat operations

    @NotBlank(message = "User ID is required")
    private String userId;
}
