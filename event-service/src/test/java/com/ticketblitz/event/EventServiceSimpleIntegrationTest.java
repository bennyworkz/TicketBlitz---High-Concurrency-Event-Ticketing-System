package com.ticketblitz.event;

import com.ticketblitz.event.dto.CreateEventRequest;
import com.ticketblitz.event.dto.EventDTO;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.entity.SeatLayout;
import com.ticketblitz.event.repository.EventRepository;
import com.ticketblitz.event.repository.SeatLayoutRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple Integration Tests for Event Service
 * Uses existing Docker containers (PostgreSQL + Redis)
 * Run: docker-compose up -d before running tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventServiceSimpleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatLayoutRepository seatLayoutRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        seatLayoutRepository.deleteAll();
        eventRepository.deleteAll();
        
        // Clear cache
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/events";
    }

    // ==================== CREATE EVENT TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Create VISUAL event should save to database and generate seats")
    void createVisualEvent_ShouldSaveAndGenerateSeats() {
        // ARRANGE
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Rock Concert 2026")
                .venue("Madison Square Garden")
                .eventDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .totalSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();

        // ACT
        ResponseEntity<EventDTO> response = restTemplate.postForEntity(
                baseUrl(), request, EventDTO.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Rock Concert 2026");
        assertThat(response.getBody().getTotalSeats()).isEqualTo(100);
        assertThat(response.getBody().getAvailableSeats()).isEqualTo(100);

        // Verify in database
        List<Event> events = eventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Rock Concert 2026");

        // Verify seats were generated
        List<SeatLayout> seats = seatLayoutRepository.findAll();
        assertThat(seats).hasSize(100);  // 10 rows × 10 seats
    }

    @Test
    @Order(2)
    @DisplayName("Create TATKAL event should save to database without generating seats")
    void createTatkalEvent_ShouldSaveWithoutSeats() {
        // ARRANGE
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Express Train")
                .venue("Railway Station")
                .eventDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .totalSeats(200)
                .bookingMode(Event.BookingMode.TATKAL)
                .build();

        // ACT
        ResponseEntity<EventDTO> response = restTemplate.postForEntity(
                baseUrl(), request, EventDTO.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalSeats()).isEqualTo(200);

        // Verify seats were NOT generated
        List<SeatLayout> seats = seatLayoutRepository.findAll();
        assertThat(seats).isEmpty();
    }

    // ==================== GET EVENT TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Get event by ID should return event from database")
    void getEventById_ShouldReturnEvent() {
        // ARRANGE: Create event first
        Event event = Event.builder()
                .name("Test Event")
                .venue("Test Venue")
                .eventDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .totalSeats(50)
                .availableSeats(50)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();
        event = eventRepository.save(event);

        // ACT
        ResponseEntity<EventDTO> response = restTemplate.getForEntity(
                baseUrl() + "/" + event.getId(), EventDTO.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(event.getId());
        assertThat(response.getBody().getName()).isEqualTo("Test Event");
    }

    @Test
    @Order(4)
    @DisplayName("Get all events should return list of events")
    void getAllEvents_ShouldReturnList() {
        // ARRANGE: Create multiple events
        eventRepository.save(Event.builder()
                .name("Event 1")
                .venue("Venue 1")
                .eventDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .totalSeats(100)
                .availableSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build());

        eventRepository.save(Event.builder()
                .name("Event 2")
                .venue("Venue 2")
                .eventDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .totalSeats(200)
                .availableSeats(200)
                .bookingMode(Event.BookingMode.TATKAL)
                .build());

        // ACT
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl(), List.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    // ==================== SEAT LAYOUT TESTS ====================

    @Test
    @Order(5)
    @DisplayName("Get seats for VISUAL event should return 100 seats")
    void getSeats_ShouldReturnSeats() {
        // ARRANGE: Create VISUAL event
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Concert with Seats")
                .venue("Arena")
                .eventDate(Instant.now().plus(20, ChronoUnit.DAYS))
                .totalSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();

        ResponseEntity<EventDTO> createResponse = restTemplate.postForEntity(
                baseUrl(), request, EventDTO.class
        );
        Long eventId = createResponse.getBody().getId();

        // ACT: Get seats
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/" + eventId + "/seats", Map.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("totalSeats")).isEqualTo(100);
    }

    // ==================== UPDATE EVENT TESTS ====================

    @Test
    @Order(6)
    @DisplayName("Update event should modify database")
    void updateEvent_ShouldModify() {
        // ARRANGE: Create event
        Event event = eventRepository.save(Event.builder()
                .name("Original Name")
                .venue("Original Venue")
                .eventDate(Instant.now().plus(25, ChronoUnit.DAYS))
                .totalSeats(80)
                .availableSeats(80)
                .bookingMode(Event.BookingMode.VISUAL)
                .build());

        // ACT: Update event
        CreateEventRequest updateRequest = CreateEventRequest.builder()
                .name("Updated Name")
                .venue("Updated Venue")
                .eventDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .totalSeats(80)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();

        HttpEntity<CreateEventRequest> requestEntity = new HttpEntity<>(updateRequest);
        ResponseEntity<EventDTO> response = restTemplate.exchange(
                baseUrl() + "/" + event.getId(),
                HttpMethod.PUT,
                requestEntity,
                EventDTO.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Updated Name");
        assertThat(response.getBody().getVenue()).isEqualTo("Updated Venue");

        // Verify in database
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getName()).isEqualTo("Updated Name");
    }

    // ==================== DELETE EVENT TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Delete event should remove from database and delete associated seats")
    void deleteEvent_ShouldRemoveEventAndSeats() {
        // ARRANGE: Create VISUAL event with seats
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Event to Delete")
                .venue("Delete Venue")
                .eventDate(Instant.now().plus(35, ChronoUnit.DAYS))
                .totalSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();

        ResponseEntity<EventDTO> createResponse = restTemplate.postForEntity(
                baseUrl(), request, EventDTO.class
        );
        Long eventId = createResponse.getBody().getId();

        // Verify seats exist
        List<SeatLayout> seatsBefore = seatLayoutRepository.findAll();
        assertThat(seatsBefore).hasSize(100);

        // ACT: Delete event
        restTemplate.delete(baseUrl() + "/" + eventId);

        // ASSERT: Event deleted
        assertThat(eventRepository.findById(eventId)).isEmpty();

        // ASSERT: Seats deleted
        List<SeatLayout> seatsAfter = seatLayoutRepository.findAll();
        assertThat(seatsAfter).isEmpty();
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Search events by name should return matching events")
    void searchByName_ShouldReturnMatchingEvents() {
        // ARRANGE: Create events
        eventRepository.save(Event.builder()
                .name("Rock Concert")
                .venue("Arena")
                .eventDate(Instant.now().plus(10, ChronoUnit.DAYS))
                .totalSeats(100)
                .availableSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build());

        eventRepository.save(Event.builder()
                .name("Jazz Concert")
                .venue("Club")
                .eventDate(Instant.now().plus(15, ChronoUnit.DAYS))
                .totalSeats(50)
                .availableSeats(50)
                .bookingMode(Event.BookingMode.VISUAL)
                .build());

        // ACT
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl() + "/search?name=concert", List.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(9)
    @DisplayName("Verify seat pricing tiers are correct")
    void verifySeatPricing() {
        // ARRANGE: Create VISUAL event
        CreateEventRequest request = CreateEventRequest.builder()
                .name("Pricing Test Event")
                .venue("Test Arena")
                .eventDate(Instant.now().plus(20, ChronoUnit.DAYS))
                .totalSeats(100)
                .bookingMode(Event.BookingMode.VISUAL)
                .build();

        ResponseEntity<EventDTO> createResponse = restTemplate.postForEntity(
                baseUrl(), request, EventDTO.class
        );
        Long eventId = createResponse.getBody().getId();

        // ACT: Get seats from database
        List<SeatLayout> seats = seatLayoutRepository.findAll();

        // ASSERT: Verify pricing tiers
        long premiumSeats = seats.stream()
                .filter(s -> s.getRowNumber().matches("[ABC]"))
                .count();
        long standardSeats = seats.stream()
                .filter(s -> s.getRowNumber().matches("[DEF]"))
                .count();
        long economySeats = seats.stream()
                .filter(s -> s.getRowNumber().matches("[G-J]"))
                .count();

        assertThat(premiumSeats).isEqualTo(30);   // 3 rows × 10 seats
        assertThat(standardSeats).isEqualTo(30);  // 3 rows × 10 seats
        assertThat(economySeats).isEqualTo(40);   // 4 rows × 10 seats
    }
}
