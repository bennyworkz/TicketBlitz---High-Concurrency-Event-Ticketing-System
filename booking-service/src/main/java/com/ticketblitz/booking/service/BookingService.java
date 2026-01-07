package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.InventoryServiceClient;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.CreateBookingRequest;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.kafka.BookingEventProducer;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.common.events.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;
    private final InventoryServiceClient inventoryServiceClient;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        log.info("Creating booking: userId={}, eventId={}, seats={}",
                request.getUserId(), request.getEventId(), request.getSeatIds());

        // Verify seats are locked by this user
        boolean seatsLocked = inventoryServiceClient.verifySeatsLocked(
                request.getEventId(), 
                request.getSeatIds(), 
                request.getUserId()
        );

        if (!seatsLocked) {
            throw new RuntimeException("Seats are not locked by this user. Please lock seats first.");
        }

        // Create booking in PENDING state
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .seatIds(request.getSeatIds())
                .amount(request.getAmount())
                .status(Booking.BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created with ID: {}", booking.getId());

        // Publish BookingCreatedEvent to Kafka
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .eventId(booking.getEventId())
                .seatIds(booking.getSeatIds())
                .amount(booking.getAmount())
                .timestamp(booking.getCreatedAt())
                .build();

        bookingEventProducer.publishBookingCreatedEvent(event);

        return BookingResponse.fromEntity(booking);
    }

    @Transactional
    public void confirmBooking(Long bookingId) {
        log.info("Confirming booking: bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            log.warn("Booking is not in PENDING state: bookingId={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        bookingRepository.save(booking);

        // ⭐ CRITICAL: Release Redis locks after confirmation
        try {
            inventoryServiceClient.releaseSeats(
                    booking.getEventId(),
                    booking.getSeatIds(),
                    booking.getUserId()
            );
            log.info("Released locks for confirmed booking: bookingId={}", bookingId);
        } catch (Exception e) {
            log.error("Failed to release locks for booking: bookingId={}", bookingId, e);
            // Don't fail the confirmation if lock release fails
            // Locks will auto-expire anyway (10 min TTL)
        }

        log.info("Booking confirmed: bookingId={}", bookingId);
    }

    @Transactional
    public void failBooking(Long bookingId, String reason) {
        log.info("Failing booking: bookingId={}, reason={}", bookingId, reason);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            log.warn("Booking is not in PENDING state: bookingId={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(Booking.BookingStatus.FAILED);
        bookingRepository.save(booking);

        // ⭐ CRITICAL: Release Redis locks so seat is available again
        try {
            inventoryServiceClient.releaseSeats(
                    booking.getEventId(),
                    booking.getSeatIds(),
                    booking.getUserId()
            );
            log.info("Released locks for failed booking: bookingId={}", bookingId);
        } catch (Exception e) {
            log.error("Failed to release locks for booking: bookingId={}", bookingId, e);
        }

        log.info("Booking failed: bookingId={}", bookingId);
    }

    @Transactional
    public void cancelBooking(Long bookingId, String userId) {
        log.info("Cancelling booking: bookingId={}, userId={}", bookingId, userId);

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new RuntimeException("Booking not found or unauthorized"));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel confirmed booking");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release seats in Inventory Service
        inventoryServiceClient.releaseSeats(
                booking.getEventId(), 
                booking.getSeatIds(), 
                booking.getUserId()
        );

        log.info("Booking cancelled: bookingId={}", bookingId);
    }

    public BookingResponse getBooking(Long bookingId, String userId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new RuntimeException("Booking not found or unauthorized"));
        return BookingResponse.fromEntity(booking);
    }

    public List<BookingResponse> getUserBookings(String userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void expireBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(Instant.now());

        if (!expiredBookings.isEmpty()) {
            log.info("Found {} expired bookings", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                booking.setStatus(Booking.BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                
                // Release seats in Inventory Service
                inventoryServiceClient.releaseSeats(
                        booking.getEventId(), 
                        booking.getSeatIds(), 
                        booking.getUserId()
                );
                
                log.info("Booking expired: bookingId={}", booking.getId());
            }
        }
    }
}
