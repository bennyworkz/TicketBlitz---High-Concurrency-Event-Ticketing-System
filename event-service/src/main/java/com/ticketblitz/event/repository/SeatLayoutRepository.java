package com.ticketblitz.event.repository;

import com.ticketblitz.event.entity.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Seat Layout Repository
 */
@Repository
public interface SeatLayoutRepository extends JpaRepository<SeatLayout, Long> {

    /**
     * Find all seats for an event
     */
    List<SeatLayout> findByEventIdOrderByRowNumberAscSeatNumberAsc(Long eventId);

    /**
     * Find available seats for an event
     */
    List<SeatLayout> findByEventIdAndStatus(Long eventId, SeatLayout.SeatStatus status);

    /**
     * Find specific seat
     */
    Optional<SeatLayout> findByEventIdAndRowNumberAndSeatNumber(Long eventId, String rowNumber, String seatNumber);

    /**
     * Count available seats
     */
    @Query("SELECT COUNT(s) FROM SeatLayout s WHERE s.eventId = :eventId AND s.status = 'AVAILABLE'")
    Long countAvailableSeats(Long eventId);

    /**
     * Delete all seats for an event
     */
    void deleteByEventId(Long eventId);
}
