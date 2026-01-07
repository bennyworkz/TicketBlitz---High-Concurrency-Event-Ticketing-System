package com.ticketblitz.payment.repository;

import com.ticketblitz.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByBookingId(Long bookingId);

    List<Transaction> findByUserId(String userId);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);
}
