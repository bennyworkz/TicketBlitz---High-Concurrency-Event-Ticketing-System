package com.ticketblitz.event.service;

import com.ticketblitz.event.dto.SeatLayoutDTO;
import com.ticketblitz.event.entity.SeatLayout;
import com.ticketblitz.event.repository.SeatLayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Seat Layout Service
 * Manages seat layouts for VISUAL booking mode events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLayoutService {

    private final SeatLayoutRepository seatLayoutRepository;

    /**
     * Generate seat layout for an event
     * Creates a grid of seats (e.g., 10 rows x 10 seats = 100 seats)
     */
    @Transactional
    public void generateSeatLayout(Long eventId, int totalSeats) {
        log.info("Generating seat layout for event {} with {} seats", eventId, totalSeats);

        List<SeatLayout> seats = new ArrayList<>();
        
        // Calculate rows and seats per row
        int seatsPerRow = 10;  // Standard: 10 seats per row
        int rows = (int) Math.ceil((double) totalSeats / seatsPerRow);

        int seatCount = 0;
        for (int row = 0; row < rows && seatCount < totalSeats; row++) {
            String rowLetter = String.valueOf((char) ('A' + row));  // A, B, C, ...

            for (int seat = 1; seat <= seatsPerRow && seatCount < totalSeats; seat++) {
                SeatLayout seatLayout = SeatLayout.builder()
                        .eventId(eventId)
                        .rowNumber(rowLetter)
                        .seatNumber(String.valueOf(seat))
                        .price(calculatePrice(rowLetter))  // Front rows more expensive
                        .status(SeatLayout.SeatStatus.AVAILABLE)
                        .build();

                seats.add(seatLayout);
                seatCount++;
            }
        }

        seatLayoutRepository.saveAll(seats);
        log.info("Generated {} seats for event {}", seats.size(), eventId);
    }

    /**
     * Calculate seat price based on row
     * Front rows (A, B, C) are more expensive
     */
    private BigDecimal calculatePrice(String row) {
        return switch (row) {
            case "A", "B", "C" -> new BigDecimal("100.00");  // Premium
            case "D", "E", "F" -> new BigDecimal("75.00");   // Standard
            default -> new BigDecimal("50.00");              // Economy
        };
    }

    /**
     * Get all seats for an event (cached)
     */
    @Cacheable(value = "seats", key = "#eventId")
    public List<SeatLayoutDTO> getSeatsByEventId(Long eventId) {
        log.info("Fetching seats from database for event: {}", eventId);
        
        return seatLayoutRepository.findByEventIdOrderByRowNumberAscSeatNumberAsc(eventId).stream()
                .map(SeatLayoutDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get available seats for an event
     */
    public List<SeatLayoutDTO> getAvailableSeats(Long eventId) {
        log.info("Fetching available seats for event: {}", eventId);
        
        return seatLayoutRepository.findByEventIdAndStatus(eventId, SeatLayout.SeatStatus.AVAILABLE).stream()
                .map(SeatLayoutDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get specific seat
     */
    public SeatLayoutDTO getSeat(Long eventId, String rowNumber, String seatNumber) {
        SeatLayout seat = seatLayoutRepository.findByEventIdAndRowNumberAndSeatNumber(eventId, rowNumber, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + rowNumber + seatNumber));
        
        return SeatLayoutDTO.fromEntity(seat);
    }

    /**
     * Count available seats
     */
    public Long countAvailableSeats(Long eventId) {
        return seatLayoutRepository.countAvailableSeats(eventId);
    }
}
