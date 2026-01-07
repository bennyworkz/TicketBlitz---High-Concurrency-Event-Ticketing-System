package com.ticketblitz.payment.kafka;

import com.ticketblitz.common.events.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Transaction;
import com.ticketblitz.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(
            topics = "booking.created",
            groupId = "payment-service-group",
            containerFactory = "bookingCreatedKafkaListenerContainerFactory"
    )
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Received BookingCreatedEvent: bookingId={}, amount={}", 
                event.getBookingId(), event.getAmount());

        try {
            // Process payment
            Transaction transaction = paymentService.processPayment(event);

            // Publish result to Kafka
            if (transaction.getStatus() == Transaction.TransactionStatus.SUCCESS) {
                paymentEventProducer.publishPaymentSuccess(transaction);
                log.info("Payment successful for booking: bookingId={}", event.getBookingId());
            } else {
                paymentEventProducer.publishPaymentFailed(transaction);
                log.warn("Payment failed for booking: bookingId={}, reason={}", 
                        event.getBookingId(), transaction.getFailureReason());
            }

        } catch (Exception e) {
            log.error("Error processing payment for booking: bookingId={}", 
                    event.getBookingId(), e);
            // In production, implement retry logic or dead letter queue
        }
    }
}
