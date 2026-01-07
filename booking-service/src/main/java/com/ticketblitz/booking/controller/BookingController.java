package com.ticketblitz.booking.controller;

import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.CreateBookingRequest;
import com.ticketblitz.booking.service.BookingService;
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

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Booking orchestration and Kafka event publishing")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Creates a new booking and publishes BookingCreated event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Booking created successfully",
            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /bookings - Creating booking for user: {}", request.getUserId());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId,
            @RequestParam String userId) {
        log.info("GET /bookings/{} - userId: {}", bookingId, userId);
        BookingResponse response = bookingService.getBooking(bookingId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable String userId) {
        log.info("GET /bookings/user/{}", userId);
        List<BookingResponse> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam String userId) {
        log.info("DELETE /bookings/{} - userId: {}", bookingId, userId);
        bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Booking Service is running");
    }
}
