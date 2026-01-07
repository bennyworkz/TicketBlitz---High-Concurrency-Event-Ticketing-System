package com.ticketblitz.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketblitz.event.entity.Event;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for creating an event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 255, message = "Event name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "Venue is required")
    @Size(min = 3, max = 255, message = "Venue must be between 3 and 255 characters")
    private String venue;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant eventDate;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Max(value = 10000, message = "Total seats cannot exceed 10000")
    private Integer totalSeats;

    @NotNull(message = "Booking mode is required")
    private Event.BookingMode bookingMode;
}
