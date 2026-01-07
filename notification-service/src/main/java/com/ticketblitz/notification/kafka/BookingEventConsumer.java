package com.ticketblitz.notification.kafka;

import com.ticketblitz.common.events.BookingConfirmedEvent;
import com.ticketblitz.common.events.PaymentFailedEvent;
import com.ticketblitz.common.events.PaymentSuccessEvent;
import com.ticketblitz.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "booking-confirmed-events", groupId = "notification-service")
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Received BookingConfirmedEvent: bookingId={}", event.getBookingId());
        
        notificationService.sendBookingConfirmation(
            event.getBookingId(),
            event.getUserId(),
            event.getEmail(),
            event.getEventName(),
            event.getEventDate()
        );
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: bookingId={}", event.getBookingId());
        
        notificationService.sendPaymentSuccess(
            event.getBookingId(),
            event.getUserId(),
            event.getEmail(),
            event.getAmount().toString()
        );
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: bookingId={}", event.getBookingId());
        
        notificationService.sendPaymentFailed(
            event.getBookingId(),
            event.getUserId(),
            event.getEmail(),
            event.getReason()
        );
    }
}
