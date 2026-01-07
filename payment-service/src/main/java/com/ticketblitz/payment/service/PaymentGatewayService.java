package com.ticketblitz.payment.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock Payment Gateway Service
 * Simulates a real payment gateway like Stripe, Razorpay, or PayPal
 */
@Service
@Slf4j
public class PaymentGatewayService {

    /**
     * Process payment through mock gateway
     * 
     * @param amount Amount to charge
     * @param userId User making payment
     * @param bookingId Booking ID
     * @return Payment result
     */
    public PaymentResult processPayment(BigDecimal amount, String userId, Long bookingId) {
        log.info("Processing payment: amount={}, userId={}, bookingId={}", amount, userId, bookingId);

        try {
            // Simulate network delay (1-2 seconds)
            Thread.sleep(1000 + (long) (Math.random() * 1000));

            // Simulate payment processing
            // 90% success rate, 10% failure rate
            boolean success = Math.random() < 0.9;

            if (success) {
                String gatewayRef = "pg_ref_" + UUID.randomUUID().toString().substring(0, 8);
                log.info("Payment successful: gatewayRef={}", gatewayRef);
                return PaymentResult.success(gatewayRef);
            } else {
                // Random failure reasons
                String[] failureReasons = {
                    "Insufficient funds",
                    "Card declined",
                    "Invalid card number",
                    "Card expired",
                    "Transaction limit exceeded"
                };
                String reason = failureReasons[(int) (Math.random() * failureReasons.length)];
                log.warn("Payment failed: reason={}", reason);
                return PaymentResult.failure(reason);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);
            return PaymentResult.failure("Payment processing interrupted");
        }
    }

    /**
     * Payment result wrapper
     */
    @Data
    @AllArgsConstructor
    public static class PaymentResult {
        private boolean success;
        private String gatewayReference;
        private String failureReason;

        public static PaymentResult success(String gatewayReference) {
            return new PaymentResult(true, gatewayReference, null);
        }

        public static PaymentResult failure(String failureReason) {
            return new PaymentResult(false, null, failureReason);
        }
    }
}
