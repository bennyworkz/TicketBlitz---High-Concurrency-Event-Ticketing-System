package com.ticketblitz.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingService bookingService;

    /**
     * Runs every minute to expire bookings that have passed their expiry time
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void cleanupExpiredBookings() {
        log.debug("Running booking cleanup job");
        bookingService.expireBookings();
    }
}
