package com.ticketblitz.payment.kafka;

import com.ticketblitz.common.events.PaymentFailedEvent;
import com.ticketblitz.common.events.PaymentSuccessEvent;
import com.ticketblitz.payment.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentSuccessEvent> paymentSuccessKafkaTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate;

    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    public void publishPaymentSuccess(Transaction transaction) {
        log.info("Publishing PaymentSuccessEvent: bookingId={}, transactionId={}", 
                transaction.getBookingId(), transaction.getId());

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .bookingId(transaction.getBookingId())
                .transactionId(transaction.getId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .gatewayReference(transaction.getGatewayReference())
                .timestamp(Instant.now())
                .build();

        paymentSuccessKafkaTemplate.send(PAYMENT_SUCCESS_TOPIC, 
                transaction.getBookingId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PaymentSuccessEvent published successfully: bookingId={}", 
                                transaction.getBookingId());
                    } else {
                        log.error("Failed to publish PaymentSuccessEvent: bookingId={}", 
                                transaction.getBookingId(), ex);
                    }
                });
    }

    public void publishPaymentFailed(Transaction transaction) {
        log.info("Publishing PaymentFailedEvent: bookingId={}, transactionId={}", 
                transaction.getBookingId(), transaction.getId());

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .bookingId(transaction.getBookingId())
                .userId(transaction.getUserId())
                .reason(transaction.getFailureReason())
                .timestamp(Instant.now())
                .build();

        paymentFailedKafkaTemplate.send(PAYMENT_FAILED_TOPIC, 
                transaction.getBookingId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PaymentFailedEvent published successfully: bookingId={}", 
                                transaction.getBookingId());
                    } else {
                        log.error("Failed to publish PaymentFailedEvent: bookingId={}", 
                                transaction.getBookingId(), ex);
                    }
                });
    }
}
