package com.ticketblitz.payment.service;

import com.ticketblitz.common.events.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Transaction;
import com.ticketblitz.payment.kafka.PaymentEventProducer;
import com.ticketblitz.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentEventProducer paymentEventProducer;

    /**
     * Process payment for a booking
     * 
     * @param event Booking created event
     * @return Transaction result
     */
    @Transactional
    public Transaction processPayment(BookingCreatedEvent event) {
        log.info("Processing payment for booking: bookingId={}, amount={}", 
                event.getBookingId(), event.getAmount());

        // Check idempotency - prevent duplicate charges
        String idempotencyKey = generateIdempotencyKey(event.getBookingId(), event.getUserId());
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            log.warn("Duplicate payment request detected: bookingId={}", event.getBookingId());
            return existing.get();
        }

        // Create transaction record (PENDING)
        Transaction transaction = Transaction.builder()
                .bookingId(event.getBookingId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency("USD")
                .paymentMethod("CARD")
                .status(Transaction.TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: transactionId={}", transaction.getTransactionId());

        // Process payment through gateway
        PaymentGatewayService.PaymentResult result = paymentGatewayService.processPayment(
                event.getAmount(),
                event.getUserId(),
                event.getBookingId()
        );

        // Update transaction based on result
        if (result.isSuccess()) {
            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transaction.setGatewayReference(result.getGatewayReference());
            log.info("Payment successful: transactionId={}, gatewayRef={}", 
                    transaction.getTransactionId(), result.getGatewayReference());
            
            // Save and publish success event
            transaction = transactionRepository.save(transaction);
            paymentEventProducer.publishPaymentSuccess(transaction);
            return transaction;
        } else {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(result.getFailureReason());
            log.warn("Payment failed: transactionId={}, reason={}", 
                    transaction.getTransactionId(), result.getFailureReason());
            
            // Save and publish failure event
            transaction = transactionRepository.save(transaction);
            paymentEventProducer.publishPaymentFailed(transaction);
            return transaction;
        }
    }

    /**
     * Get transaction by ID
     */
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    /**
     * Get transactions for a booking
     */
    public List<Transaction> getTransactionsByBooking(Long bookingId) {
        return transactionRepository.findByBookingId(bookingId);
    }

    /**
     * Get transactions for a user
     */
    public List<Transaction> getTransactionsByUser(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Generate idempotency key to prevent duplicate charges
     */
    private String generateIdempotencyKey(Long bookingId, String userId) {
        return String.format("booking_%d_user_%s", bookingId, userId);
    }
}
