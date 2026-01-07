package com.ticketblitz.payment.dto;

import com.ticketblitz.payment.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String transactionId;
    private Long bookingId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private Transaction.TransactionStatus status;
    private String gatewayReference;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .bookingId(transaction.getBookingId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .gatewayReference(transaction.getGatewayReference())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
