package com.ticketblitz.booking;

import com.ticketblitz.booking.client.InventoryServiceClient;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.CreateBookingRequest;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.kafka.BookingEventProducer;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.booking.service.BookingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Booking Service
 * 
 * Tests:
 * 1. Complete booking flow
 * 2. Booking creation with seat verification
 * 3. Booking confirmation
 * 4. Booking cancellation with seat release
 * 5. Booking expiry with cleanup job
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @MockBean
    private BookingEventProducer bookingEventProducer;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    // ==================== TEST 1: CREATE BOOKING WITH SEAT VERIFICATION ====================

    @Test
    @Order(1)
    @DisplayName("Should create booking when seats are locked by user")
    void createBooking_WithLockedSeats_ShouldSucceed() {
        // ARRANGE
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-123")
                .eventId(1L)
                .seatIds(Arrays.asList("A1", "A2"))
                .amount(new BigDecimal("200.00"))
                .build();

        // ACT
        BookingResponse response = bookingService.createBooking(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getEventId()).isEqualTo(1L);
        assertThat(response.getSeatIds()).containsExactly("A1", "A2");
        assertThat(response.getAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getExpiresAt()).isNotNull();

        // Verify seat verification was called
        verify(inventoryServiceClient, times(1))
                .verifySeatsLocked(1L, Arrays.asList("A1", "A2"), "user-123");

        // Verify booking saved to database
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create booking when seats are not locked")
    void createBooking_WithoutLockedSeats_ShouldFail() {
        // ARRANGE
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(false);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-456")
                .eventId(1L)
                .seatIds(Arrays.asList("B1"))
                .amount(new BigDecimal("100.00"))
                .build();

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(request);
        });

        assertThat(exception.getMessage())
                .contains("Seats are not locked by this user");

        // Verify no booking was created
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).isEmpty();
    }

    // ==================== TEST 2: BOOKING CONFIRMATION ====================

    @Test
    @Order(3)
    @DisplayName("Should confirm booking when confirmBooking is called")
    void confirmBooking_ShouldUpdateStatus() {
        // ARRANGE: Create a booking first
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-789")
                .eventId(1L)
                .seatIds(Arrays.asList("C1"))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();

        // ACT: Confirm the booking
        bookingService.confirmBooking(bookingId);

        // ASSERT: Verify booking is confirmed
        Booking confirmedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(confirmedBooking.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(confirmedBooking.getConfirmedAt()).isNotNull();
    }

    // ==================== TEST 3: CANCEL BOOKING WITH SEAT RELEASE ====================

    @Test
    @Order(4)
    @DisplayName("Should cancel booking and release seats")
    @Transactional
    void cancelBooking_ShouldReleaseSeats() {
        // ARRANGE: Create a booking
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-999")
                .eventId(1L)
                .seatIds(new ArrayList<>(Arrays.asList("D1", "D2")))
                .amount(new BigDecimal("200.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();

        // ACT: Cancel the booking
        bookingService.cancelBooking(bookingId, "user-999");

        // ASSERT: Verify booking is cancelled
        Booking cancelledBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);

        // Verify seats were released
        verify(inventoryServiceClient, times(1))
                .releaseSeats(1L, Arrays.asList("D1", "D2"), "user-999");
    }

    @Test
    @Order(5)
    @DisplayName("Should not cancel confirmed booking")
    void cancelBooking_WhenConfirmed_ShouldFail() {
        // ARRANGE: Create and confirm a booking
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-111")
                .eventId(1L)
                .seatIds(Arrays.asList("E1"))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();

        // Manually confirm the booking
        bookingService.confirmBooking(bookingId);

        // ACT & ASSERT: Try to cancel confirmed booking
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.cancelBooking(bookingId, "user-111");
        });

        assertThat(exception.getMessage()).contains("Cannot cancel confirmed booking");
    }

    // ==================== TEST 4: GET BOOKING ====================

    @Test
    @Order(6)
    @DisplayName("Should get booking by ID and user")
    void getBooking_WithValidIdAndUser_ShouldReturnBooking() {
        // ARRANGE: Create a booking
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-222")
                .eventId(1L)
                .seatIds(Arrays.asList("F1"))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse createdBooking = bookingService.createBooking(request);

        // ACT: Get the booking
        BookingResponse retrievedBooking = bookingService.getBooking(
                createdBooking.getId(), 
                "user-222"
        );

        // ASSERT
        assertThat(retrievedBooking).isNotNull();
        assertThat(retrievedBooking.getId()).isEqualTo(createdBooking.getId());
        assertThat(retrievedBooking.getUserId()).isEqualTo("user-222");
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to get booking with wrong user")
    void getBooking_WithWrongUser_ShouldFail() {
        // ARRANGE: Create a booking
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-333")
                .eventId(1L)
                .seatIds(Arrays.asList("G1"))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);

        // ACT & ASSERT: Try to get booking with different user
        assertThrows(RuntimeException.class, () -> {
            bookingService.getBooking(booking.getId(), "wrong-user");
        });
    }

    // ==================== TEST 5: GET USER BOOKINGS ====================

    @Test
    @Order(8)
    @DisplayName("Should get all bookings for a user")
    void getUserBookings_ShouldReturnAllUserBookings() {
        // ARRANGE: Create multiple bookings for same user
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request1 = CreateBookingRequest.builder()
                .userId("user-444")
                .eventId(1L)
                .seatIds(Arrays.asList("H1"))
                .amount(new BigDecimal("100.00"))
                .build();

        CreateBookingRequest request2 = CreateBookingRequest.builder()
                .userId("user-444")
                .eventId(2L)
                .seatIds(Arrays.asList("I1", "I2"))
                .amount(new BigDecimal("200.00"))
                .build();

        bookingService.createBooking(request1);
        bookingService.createBooking(request2);

        // ACT: Get all bookings for user
        List<BookingResponse> bookings = bookingService.getUserBookings("user-444");

        // ASSERT
        assertThat(bookings).hasSize(2);
        assertThat(bookings).allMatch(b -> b.getUserId().equals("user-444"));
    }

    // ==================== TEST 6: EXPIRE BOOKINGS ====================

    @Test
    @Order(9)
    @DisplayName("Should expire bookings past expiry time")
    @Transactional
    void expireBookings_ShouldMarkAsExpiredAndReleaseSeats() {
        // ARRANGE: Create a booking
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-555")
                .eventId(1L)
                .seatIds(new ArrayList<>(Arrays.asList("J1")))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();

        // Manually set expiry time to past
        Booking bookingEntity = bookingRepository.findById(bookingId).orElseThrow();
        bookingEntity.setExpiresAt(Instant.now().minusSeconds(60));
        bookingRepository.save(bookingEntity);

        // ACT: Run expiry job
        bookingService.expireBookings();

        // ASSERT: Verify booking is expired
        Booking expiredBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(expiredBooking.getStatus()).isEqualTo(Booking.BookingStatus.EXPIRED);

        // Verify seats were released
        verify(inventoryServiceClient, times(1))
                .releaseSeats(1L, Arrays.asList("J1"), "user-555");
    }

    @Test
    @Order(10)
    @DisplayName("Should not expire bookings that haven't expired yet")
    void expireBookings_WithValidBookings_ShouldNotExpire() {
        // ARRANGE: Create a booking (not expired)
        when(inventoryServiceClient.verifySeatsLocked(anyLong(), anyList(), anyString()))
                .thenReturn(true);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId("user-666")
                .eventId(1L)
                .seatIds(Arrays.asList("K1"))
                .amount(new BigDecimal("100.00"))
                .build();

        BookingResponse booking = bookingService.createBooking(request);
        Long bookingId = booking.getId();

        // ACT: Run expiry job
        bookingService.expireBookings();

        // ASSERT: Verify booking is still PENDING
        Booking stillPendingBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(stillPendingBooking.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
    }
}
