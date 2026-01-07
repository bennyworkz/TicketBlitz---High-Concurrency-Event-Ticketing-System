package com.ticketblitz.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketblitz.event.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO implements Serializable {

    private Long id;
    private String name;
    private String venue;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant eventDate;
    
    private Integer totalSeats;
    private Integer availableSeats;
    private Event.BookingMode bookingMode;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    /**
     * Convert Entity to DTO
     */
    public static EventDTO fromEntity(Event event) {
        return EventDTO.builder()
                .id(event.getId())
                .name(event.getName())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .bookingMode(event.getBookingMode())
                .createdAt(event.getCreatedAt())
                .build();
    }

    /**
     * Convert DTO to Entity
     */
    public Event toEntity() {
        return Event.builder()
                .id(this.id)
                .name(this.name)
                .venue(this.venue)
                .eventDate(this.eventDate)
                .totalSeats(this.totalSeats)
                .availableSeats(this.availableSeats)
                .bookingMode(this.bookingMode)
                .createdAt(this.createdAt)
                .build();
    }
}
