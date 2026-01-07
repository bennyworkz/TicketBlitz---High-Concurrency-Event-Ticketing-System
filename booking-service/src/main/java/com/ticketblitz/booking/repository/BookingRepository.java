package com.ticketblitz.booking.repository;

import com.ticketblitz.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByEventId(Long eventId);

    Optional<Booking> findByIdAndUserId(Long id, String userId);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") Instant now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.eventId = :eventId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsByEventId(@Param("eventId") Long eventId);
}
