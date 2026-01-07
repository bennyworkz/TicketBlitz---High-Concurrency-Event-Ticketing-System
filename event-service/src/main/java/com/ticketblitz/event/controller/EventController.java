package com.ticketblitz.event.controller;

import com.ticketblitz.event.dto.CreateEventRequest;
import com.ticketblitz.event.dto.EventDTO;
import com.ticketblitz.event.dto.SeatLayoutDTO;
import com.ticketblitz.event.service.EventService;
import com.ticketblitz.event.service.SeatLayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event Controller
 * REST API for event management
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "Event management and seat layout APIs")
public class EventController {

    private final EventService eventService;
    private final SeatLayoutService seatLayoutService;

    /**
     * Create new event
     * POST /events
     */
    @PostMapping
    @Operation(summary = "Create new event", description = "Creates a new event with seat layout")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully",
            content = @Content(schema = @Schema(implementation = EventDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody CreateEventRequest request) {
        log.info("Create event request: {}", request.getName());
        EventDTO event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * Get event by ID
     * GET /events/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieves event details from cache or database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event found",
            content = @Content(schema = @Schema(implementation = EventDTO.class))),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventDTO> getEvent(
            @Parameter(description = "Event ID") @PathVariable Long id) {
        log.info("Get event request: {}", id);
        EventDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Get all events
     * GET /events
     */
    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieves all events with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    })
    public ResponseEntity<List<EventDTO>> getAllEvents(
            @Parameter(description = "Filter: upcoming, available, or null for all")
            @RequestParam(required = false) String filter) {
        
        log.info("Get all events request, filter: {}", filter);
        
        List<EventDTO> events;
        if ("upcoming".equals(filter)) {
            events = eventService.getUpcomingEvents();
        } else if ("available".equals(filter)) {
            events = eventService.getAvailableEvents();
        } else {
            events = eventService.getAllEvents();
        }
        
        return ResponseEntity.ok(events);
    }

    /**
     * Search events
     * GET /events/search?name=concert&venue=stadium
     */
    @GetMapping("/search")
    public ResponseEntity<List<EventDTO>> searchEvents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String venue) {
        
        log.info("Search events - name: {}, venue: {}", name, venue);
        
        List<EventDTO> events;
        if (name != null && !name.isEmpty()) {
            events = eventService.searchEventsByName(name);
        } else if (venue != null && !venue.isEmpty()) {
            events = eventService.searchEventsByVenue(venue);
        } else {
            events = eventService.getAllEvents();
        }
        
        return ResponseEntity.ok(events);
    }

    /**
     * Get seat layout for an event
     * GET /events/{id}/seats
     */
    @GetMapping("/{id}/seats")
    public ResponseEntity<Map<String, Object>> getSeats(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean availableOnly) {
        
        log.info("Get seats for event: {}, availableOnly: {}", id, availableOnly);
        
        // Get event details
        EventDTO event = eventService.getEventById(id);
        
        // Get seats
        List<SeatLayoutDTO> seats;
        if (availableOnly) {
            seats = seatLayoutService.getAvailableSeats(id);
        } else {
            seats = seatLayoutService.getSeatsByEventId(id);
        }
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("event", event);
        response.put("seats", seats);
        response.put("totalSeats", seats.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update event
     * PUT /events/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateEventRequest request) {
        
        log.info("Update event request: {}", id);
        EventDTO event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(event);
    }

    /**
     * Delete event
     * DELETE /events/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id) {
        log.info("Delete event request: {}", id);
        eventService.deleteEvent(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Event deleted successfully");
        response.put("eventId", id.toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     * GET /events/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "event-service");
        return ResponseEntity.ok(response);
    }
}
