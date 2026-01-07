package com.ticketblitz.payment;

import com.ticketblitz.common.events.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Transaction;
import com.ticketblitz.payment.kafka.PaymentEventProducer;
import com.ticketblitz.payment.repository.TransactionRepository;
import com.ticketblitz.payment.service.PaymentGatewayService;
import com.ticketblitz.payment.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Payment Service
 * 
 * Tests:
 * 1. Successful payment processing
 * 2. Failed payment processing
 * 3. Idempotency (duplicate requests)
 * 4. Transaction persistence
 * 5. Event publishing
 * 6. Gateway timeout handling
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TransactionRepository transactionRepository;

    @SpyBean
    private PaymentGatewayService paymentGatewayService;

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    // ==================== TEST 1: SUCCESSFUL PAYMENT ====================

    @Test
    @Order(1)
    @DisplayName("Should process payment successfully")
    void processPayment_Success_ShouldCreateSuccessfulTransaction() {
        // ARRANGE
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(1L)
                .userId("user-123")
                .eventId(1L)
                .seatIds(Arrays.asList("A1", "A2"))
                .amount(new BigDecimal("200.00"))
                .timestamp(Instant.now())
                .build();

        // Mock gateway to return success
        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_xyz123"));

        // ACT
        Transaction transaction = paymentService.processPayment(event);

        // ASSERT
        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getTransactionId()).isNotNull();
        assertThat(transaction.getBookingId()).isEqualTo(1L);
        assertThat(transaction.getUserId()).isEqualTo("user-123");
        assertThat(transaction.getAmount()).isEqualByComparingTo("200.00");
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(transaction.getGatewayReference()).isEqualTo("pg_ref_xyz123");
        assertThat(transaction.getFailureReason()).isNull();
        assertThat(transaction.getCreatedAt()).isNotNull();

        // Verify gateway was called
        verify(paymentGatewayService, times(1))
                .processPayment(new BigDecimal("200.00"), "user-123", 1L);

        // Verify event was published
        verify(paymentEventProducer, times(1))
                .publishPaymentSuccess(any(Transaction.class));

        // Verify saved to database
        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
    }

    // ==================== TEST 2: FAILED PAYMENT ====================

    @Test
    @Order(2)
    @DisplayName("Should handle payment failure")
    void processPayment_Failure_ShouldCreateFailedTransaction() {
        // ARRANGE
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(2L)
                .userId("user-456")
                .eventId(1L)
                .seatIds(Arrays.asList("B1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        // Mock gateway to return failure
        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.failure("Card declined"));

        // ACT
        Transaction transaction = paymentService.processPayment(event);

        // ASSERT
        assertThat(transaction).isNotNull();
        assertThat(transaction.getBookingId()).isEqualTo(2L);
        assertThat(transaction.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
        assertThat(transaction.getGatewayReference()).isNull();
        assertThat(transaction.getFailureReason()).isEqualTo("Card declined");

        // Verify event was published
        verify(paymentEventProducer, times(1))
                .publishPaymentFailed(any(Transaction.class));

        // Verify saved to database
        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
    }

    // ==================== TEST 3: IDEMPOTENCY ====================

    @Test
    @Order(3)
    @DisplayName("Should handle duplicate payment requests (idempotency)")
    void processPayment_Duplicate_ShouldReturnExistingTransaction() {
        // ARRANGE
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(3L)
                .userId("user-789")
                .eventId(1L)
                .seatIds(Arrays.asList("C1"))
                .amount(new BigDecimal("150.00"))
                .timestamp(Instant.now())
                .build();

        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_abc"));

        // ACT: Process payment first time
        Transaction firstTransaction = paymentService.processPayment(event);

        // ACT: Process same payment again (duplicate)
        Transaction secondTransaction = paymentService.processPayment(event);

        // ASSERT: Should return same transaction
        assertThat(secondTransaction.getId()).isEqualTo(firstTransaction.getId());
        assertThat(secondTransaction.getTransactionId()).isEqualTo(firstTransaction.getTransactionId());

        // Verify gateway was called only ONCE (not twice)
        verify(paymentGatewayService, times(1))
                .processPayment(any(), any(), any());

        // Verify only ONE transaction in database
        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
    }

    // ==================== TEST 4: GET TRANSACTION BY ID ====================

    @Test
    @Order(4)
    @DisplayName("Should get transaction by transaction ID")
    void getTransaction_WithValidId_ShouldReturnTransaction() {
        // ARRANGE: Create a transaction
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(4L)
                .userId("user-111")
                .eventId(1L)
                .seatIds(Arrays.asList("D1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_def"));

        Transaction created = paymentService.processPayment(event);

        // ACT: Get transaction by ID
        Transaction retrieved = paymentService.getTransaction(created.getTransactionId()).orElse(null);

        // ASSERT
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTransactionId()).isEqualTo(created.getTransactionId());
        assertThat(retrieved.getBookingId()).isEqualTo(4L);
    }

    // ==================== TEST 5: GET TRANSACTIONS BY BOOKING ====================

    @Test
    @Order(5)
    @DisplayName("Should get all transactions for a booking")
    void getTransactionsByBooking_ShouldReturnAllTransactions() {
        // ARRANGE: Create multiple transactions for same booking
        BookingCreatedEvent event1 = BookingCreatedEvent.builder()
                .bookingId(5L)
                .userId("user-222")
                .eventId(1L)
                .seatIds(Arrays.asList("E1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.failure("First attempt failed"))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_ghi"));

        // First attempt (failed)
        paymentService.processPayment(event1);

        // Second attempt (success) - different idempotency key
        BookingCreatedEvent event2 = BookingCreatedEvent.builder()
                .bookingId(5L)
                .userId("user-222-retry")  // Different user to bypass idempotency
                .eventId(1L)
                .seatIds(Arrays.asList("E1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        paymentService.processPayment(event2);

        // ACT: Get all transactions for booking
        List<Transaction> transactions = paymentService.getTransactionsByBooking(5L);

        // ASSERT
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getBookingId)
                .containsOnly(5L);
    }

    // ==================== TEST 6: GET TRANSACTIONS BY USER ====================

    @Test
    @Order(6)
    @DisplayName("Should get all transactions for a user")
    void getTransactionsByUser_ShouldReturnAllUserTransactions() {
        // ARRANGE: Create multiple transactions for same user
        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_1"))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_2"));

        BookingCreatedEvent event1 = BookingCreatedEvent.builder()
                .bookingId(6L)
                .userId("user-333")
                .eventId(1L)
                .seatIds(Arrays.asList("F1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        BookingCreatedEvent event2 = BookingCreatedEvent.builder()
                .bookingId(7L)
                .userId("user-333")
                .eventId(2L)
                .seatIds(Arrays.asList("G1"))
                .amount(new BigDecimal("150.00"))
                .timestamp(Instant.now())
                .build();

        paymentService.processPayment(event1);
        paymentService.processPayment(event2);

        // ACT: Get all transactions for user
        List<Transaction> transactions = paymentService.getTransactionsByUser("user-333");

        // ASSERT
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getUserId)
                .containsOnly("user-333");
        assertThat(transactions).extracting(Transaction::getBookingId)
                .containsExactlyInAnyOrder(6L, 7L);
    }

    // ==================== TEST 7: TRANSACTION PERSISTENCE ====================

    @Test
    @Order(7)
    @DisplayName("Should persist transaction with all fields")
    void processPayment_ShouldPersistAllFields() {
        // ARRANGE
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(8L)
                .userId("user-444")
                .eventId(1L)
                .seatIds(Arrays.asList("H1"))
                .amount(new BigDecimal("175.50"))
                .timestamp(Instant.now())
                .build();

        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_jkl"));

        // ACT
        Transaction transaction = paymentService.processPayment(event);

        // ASSERT: Verify all fields are persisted
        Transaction persisted = transactionRepository.findById(transaction.getId()).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTransactionId()).isNotNull();
        assertThat(persisted.getBookingId()).isEqualTo(8L);
        assertThat(persisted.getUserId()).isEqualTo("user-444");
        assertThat(persisted.getAmount()).isEqualByComparingTo("175.50");
        assertThat(persisted.getCurrency()).isEqualTo("USD");
        assertThat(persisted.getPaymentMethod()).isEqualTo("CARD");
        assertThat(persisted.getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(persisted.getGatewayReference()).isEqualTo("pg_ref_jkl");
        assertThat(persisted.getIdempotencyKey()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

    // ==================== TEST 8: IDEMPOTENCY KEY GENERATION ====================

    @Test
    @Order(8)
    @DisplayName("Should generate unique idempotency keys")
    void processPayment_ShouldGenerateUniqueIdempotencyKeys() {
        // ARRANGE
        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_1"))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref_2"));

        BookingCreatedEvent event1 = BookingCreatedEvent.builder()
                .bookingId(9L)
                .userId("user-555")
                .eventId(1L)
                .seatIds(Arrays.asList("I1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        BookingCreatedEvent event2 = BookingCreatedEvent.builder()
                .bookingId(10L)
                .userId("user-555")
                .eventId(1L)
                .seatIds(Arrays.asList("J1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        // ACT
        Transaction transaction1 = paymentService.processPayment(event1);
        Transaction transaction2 = paymentService.processPayment(event2);

        // ASSERT: Different bookings should have different idempotency keys
        assertThat(transaction1.getIdempotencyKey())
                .isNotEqualTo(transaction2.getIdempotencyKey());
        assertThat(transaction1.getIdempotencyKey()).contains("booking_9");
        assertThat(transaction2.getIdempotencyKey()).contains("booking_10");
    }

    // ==================== TEST 9: PAYMENT AMOUNTS ====================

    @Test
    @Order(9)
    @DisplayName("Should handle different payment amounts correctly")
    void processPayment_WithDifferentAmounts_ShouldPersistCorrectly() {
        // ARRANGE
        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref"));

        BigDecimal[] amounts = {
                new BigDecimal("50.00"),
                new BigDecimal("100.50"),
                new BigDecimal("999.99"),
                new BigDecimal("1500.00")
        };

        // ACT & ASSERT
        for (int i = 0; i < amounts.length; i++) {
            BookingCreatedEvent event = BookingCreatedEvent.builder()
                    .bookingId((long) (11 + i))
                    .userId("user-666")
                    .eventId(1L)
                    .seatIds(Arrays.asList("K" + (i + 1)))
                    .amount(amounts[i])
                    .timestamp(Instant.now())
                    .build();

            Transaction transaction = paymentService.processPayment(event);
            assertThat(transaction.getAmount()).isEqualByComparingTo(amounts[i]);
        }
    }

    // ==================== TEST 10: TRANSACTION STATUS TRANSITIONS ====================

    @Test
    @Order(10)
    @DisplayName("Should track transaction status correctly")
    void processPayment_ShouldTrackStatusCorrectly() {
        // ARRANGE
        BookingCreatedEvent successEvent = BookingCreatedEvent.builder()
                .bookingId(15L)
                .userId("user-777")
                .eventId(1L)
                .seatIds(Arrays.asList("L1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        BookingCreatedEvent failureEvent = BookingCreatedEvent.builder()
                .bookingId(16L)
                .userId("user-888")
                .eventId(1L)
                .seatIds(Arrays.asList("M1"))
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();

        when(paymentGatewayService.processPayment(any(), any(), any()))
                .thenReturn(PaymentGatewayService.PaymentResult.success("pg_ref"))
                .thenReturn(PaymentGatewayService.PaymentResult.failure("Insufficient funds"));

        // ACT
        Transaction successTransaction = paymentService.processPayment(successEvent);
        Transaction failureTransaction = paymentService.processPayment(failureEvent);

        // ASSERT
        assertThat(successTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(successTransaction.getGatewayReference()).isNotNull();
        assertThat(successTransaction.getFailureReason()).isNull();

        assertThat(failureTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
        assertThat(failureTransaction.getGatewayReference()).isNull();
        assertThat(failureTransaction.getFailureReason()).isEqualTo("Insufficient funds");
    }
}
