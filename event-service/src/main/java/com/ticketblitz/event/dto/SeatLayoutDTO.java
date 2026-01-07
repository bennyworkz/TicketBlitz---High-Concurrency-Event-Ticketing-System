package com.ticketblitz.event.dto;

import com.ticketblitz.event.entity.SeatLayout;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Seat Layout Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutDTO implements Serializable {

    private Long id;
    private Long eventId;
    private String rowNumber;
    private String seatNumber;
    private String seatIdentifier;  // e.g., "A1"
    private BigDecimal price;
    private SeatLayout.SeatStatus status;

    /**
     * Convert Entity to DTO
     */
    public static SeatLayoutDTO fromEntity(SeatLayout seat) {
        return SeatLayoutDTO.builder()
                .id(seat.getId())
                .eventId(seat.getEventId())
                .rowNumber(seat.getRowNumber())
                .seatNumber(seat.getSeatNumber())
                .seatIdentifier(seat.getSeatIdentifier())
                .price(seat.getPrice())
                .status(seat.getStatus())
                .build();
    }

    /**
     * Convert DTO to Entity
     */
    public SeatLayout toEntity() {
        return SeatLayout.builder()
                .id(this.id)
                .eventId(this.eventId)
                .rowNumber(this.rowNumber)
                .seatNumber(this.seatNumber)
                .price(this.price)
                .status(this.status)
                .build();
    }
}
