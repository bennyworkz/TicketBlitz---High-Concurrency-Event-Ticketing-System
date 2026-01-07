package com.ticketblitz.event.initializer;

import com.ticketblitz.event.dto.CreateEventRequest;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Data Initializer
 * Seeds the database with test data on startup (disabled in tests)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "data.initializer.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class DataInitializer implements CommandLineRunner {

    private final EventService eventService;

    @Override
    public void run(String... args) {
        log.info("Initializing test data...");

        try {
            // Create a sample event with 100 seats
            CreateEventRequest request = CreateEventRequest.builder()
                    .name("Rock Concert 2025")
                    .venue("Madison Square Garden")
                    .eventDate(Instant.now().plus(30, ChronoUnit.DAYS))  // 30 days from now
                    .totalSeats(100)
                    .bookingMode(Event.BookingMode.VISUAL)
                    .build();

            eventService.createEvent(request);
            log.info("✅ Test data initialized: 1 event with 100 seats created");

            // Create a Tatkal mode event
            CreateEventRequest tatkalRequest = CreateEventRequest.builder()
                    .name("Express Train to Mumbai")
                    .venue("Central Railway Station")
                    .eventDate(Instant.now().plus(7, ChronoUnit.DAYS))  // 7 days from now
                    .totalSeats(200)
                    .bookingMode(Event.BookingMode.TATKAL)
                    .build();

            eventService.createEvent(tatkalRequest);
            log.info("✅ Test data initialized: 1 Tatkal event with 200 seats created");

        } catch (Exception e) {
            log.warn("Test data already exists or error occurred: {}", e.getMessage());
        }
    }
}
