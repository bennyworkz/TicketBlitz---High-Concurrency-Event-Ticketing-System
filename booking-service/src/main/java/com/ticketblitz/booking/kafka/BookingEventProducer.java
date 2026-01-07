package com.ticketblitz.booking.kafka;

import com.ticketblitz.common.events.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;
    private static final String BOOKING_CREATED_TOPIC = "booking.created";

    public void publishBookingCreatedEvent(BookingCreatedEvent event) {
        log.info("Publishing BookingCreatedEvent: bookingId={}, userId={}, amount={}",
                event.getBookingId(), event.getUserId(), event.getAmount());

        kafkaTemplate.send(BOOKING_CREATED_TOPIC, event.getBookingId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("BookingCreatedEvent published successfully: bookingId={}",
                                event.getBookingId());
                    } else {
                        log.error("Failed to publish BookingCreatedEvent: bookingId={}",
                                event.getBookingId(), ex);
                    }
                });
    }
}
