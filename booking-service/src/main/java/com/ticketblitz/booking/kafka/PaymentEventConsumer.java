package com.ticketblitz.booking.kafka;

import com.ticketblitz.booking.service.BookingService;
import com.ticketblitz.common.events.PaymentSuccessEvent;
import com.ticketblitz.common.events.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final BookingService bookingService;

    @KafkaListener(
            topics = "payment.success",
            groupId = "booking-service-group",
            containerFactory = "paymentSuccessKafkaListenerContainerFactory"
    )
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: bookingId={}, transactionId={}",
                event.getBookingId(), event.getTransactionId());

        try {
            bookingService.confirmBooking(event.getBookingId());
            log.info("Booking confirmed successfully: bookingId={}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to confirm booking: bookingId={}", event.getBookingId(), e);
            // In production, you'd implement retry logic or dead letter queue
        }
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "booking-service-group",
            containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: bookingId={}, reason={}",
                event.getBookingId(), event.getReason());

        try {
            bookingService.failBooking(event.getBookingId(), event.getReason());
            log.info("Booking failed successfully: bookingId={}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to process payment failure: bookingId={}", event.getBookingId(), e);
        }
    }
}
