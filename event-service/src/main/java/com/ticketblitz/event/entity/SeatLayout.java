package com.ticketblitz.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Seat Layout Entity
 * Represents individual seats for VISUAL booking mode events
 */
@Entity
@Table(name = "seat_layouts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "row_number", "seat_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayout implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "row_number", nullable = false, length = 5)
    private String rowNumber;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    /**
     * Seat Status:
     * - AVAILABLE: Can be booked
     * - LOCKED: Temporarily locked during booking process
     * - BOOKED: Permanently booked
     */
    public enum SeatStatus {
        AVAILABLE,
        LOCKED,
        BOOKED
    }

    /**
     * Get seat identifier (e.g., "A1", "B5")
     */
    public String getSeatIdentifier() {
        return rowNumber + seatNumber;
    }
}
