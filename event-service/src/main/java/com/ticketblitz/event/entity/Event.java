package com.ticketblitz.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event Entity
 * Represents a ticketed event (concert, movie, sports, etc.)
 */
@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    /**
     * Booking Mode:
     * - VISUAL: Users select specific seats (cinema, theater)
     * - TATKAL: First-come-first-served, no seat selection (train, concert)
     */
    @Column(name = "booking_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingMode bookingMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (availableSeats == null) {
            availableSeats = totalSeats;
        }
    }

    public enum BookingMode {
        VISUAL,  // Seat selection (cinema, theater)
        TATKAL   // First-come-first-served (train, concert)
    }
}
