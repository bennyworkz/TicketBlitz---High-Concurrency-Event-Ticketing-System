package com.ticketblitz.event.repository;

import com.ticketblitz.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Event Repository
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Find upcoming events
     */
    List<Event> findByEventDateAfterOrderByEventDateAsc(Instant date);

    /**
     * Search events by name (case-insensitive)
     */
    List<Event> findByNameContainingIgnoreCase(String name);

    /**
     * Find events by venue
     */
    List<Event> findByVenueContainingIgnoreCase(String venue);

    /**
     * Find events with available seats
     */
    @Query("SELECT e FROM Event e WHERE e.availableSeats > 0 AND e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findAvailableEvents(Instant now);
}
