package com.ticketblitz.event.service;

import com.ticketblitz.event.dto.CreateEventRequest;
import com.ticketblitz.event.dto.EventDTO;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.repository.EventRepository;
import com.ticketblitz.event.repository.SeatLayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event Service
 * Handles event CRUD operations with Redis caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final SeatLayoutService seatLayoutService;

    /**
     * Create new event
     */
    @Transactional
    public EventDTO createEvent(CreateEventRequest request) {
        log.info("Creating event: {}", request.getName());

        Event event = Event.builder()
                .name(request.getName())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .bookingMode(request.getBookingMode())
                .build();

        event = eventRepository.save(event);
        log.info("Event created with ID: {}", event.getId());

        // For VISUAL mode, create seat layout
        if (event.getBookingMode() == Event.BookingMode.VISUAL) {
            seatLayoutService.generateSeatLayout(event.getId(), request.getTotalSeats());
        }

        return EventDTO.fromEntity(event);
    }

    /**
     * Get event by ID (cached)
     */
    @Cacheable(value = "events", key = "#id")
    public EventDTO getEventById(Long id) {
        log.info("Fetching event from database: {}", id);
        
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        
        return EventDTO.fromEntity(event);
    }

    /**
     * Get all events
     */
    public List<EventDTO> getAllEvents() {
        log.info("Fetching all events");
        
        return eventRepository.findAll().stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming events
     */
    public List<EventDTO> getUpcomingEvents() {
        log.info("Fetching upcoming events");
        
        return eventRepository.findByEventDateAfterOrderByEventDateAsc(Instant.now()).stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get available events (with seats)
     */
    public List<EventDTO> getAvailableEvents() {
        log.info("Fetching available events");
        
        return eventRepository.findAvailableEvents(Instant.now()).stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search events by name
     */
    public List<EventDTO> searchEventsByName(String name) {
        log.info("Searching events by name: {}", name);
        
        return eventRepository.findByNameContainingIgnoreCase(name).stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search events by venue
     */
    public List<EventDTO> searchEventsByVenue(String venue) {
        log.info("Searching events by venue: {}", venue);
        
        return eventRepository.findByVenueContainingIgnoreCase(venue).stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update event (invalidates cache)
     */
    @Transactional
    @CacheEvict(value = "events", key = "#id")
    public EventDTO updateEvent(Long id, CreateEventRequest request) {
        log.info("Updating event: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        event.setName(request.getName());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        // Note: Don't update totalSeats or bookingMode after creation

        event = eventRepository.save(event);
        log.info("Event updated: {}", id);

        return EventDTO.fromEntity(event);
    }

    /**
     * Delete event (invalidates cache)
     */
    @Transactional
    @CacheEvict(value = "events", key = "#id")
    public void deleteEvent(Long id) {
        log.info("Deleting event: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        // Delete seat layouts first (if VISUAL mode)
        if (event.getBookingMode() == Event.BookingMode.VISUAL) {
            seatLayoutRepository.deleteByEventId(id);
        }

        eventRepository.delete(event);
        log.info("Event deleted: {}", id);
    }

    /**
     * Update available seats count
     */
    @Transactional
    @CacheEvict(value = "events", key = "#eventId")
    public void updateAvailableSeats(Long eventId, int delta) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        event.setAvailableSeats(event.getAvailableSeats() + delta);
        eventRepository.save(event);
        
        log.info("Updated available seats for event {}: {} -> {}", 
                eventId, event.getAvailableSeats() - delta, event.getAvailableSeats());
    }
}
