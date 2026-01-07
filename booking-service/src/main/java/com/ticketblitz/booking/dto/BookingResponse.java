package com.ticketblitz.booking.dto;

import com.ticketblitz.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String userId;
    private Long eventId;
    private List<String> seatIds;
    private BigDecimal amount;
    private Booking.BookingStatus status;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant expiresAt;

    public static BookingResponse fromEntity(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .eventId(booking.getEventId())
                .seatIds(booking.getSeatIds())
                .amount(booking.getAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .confirmedAt(booking.getConfirmedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }
}
