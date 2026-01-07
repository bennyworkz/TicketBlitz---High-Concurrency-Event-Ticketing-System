package com.ticketblitz.payment.controller;

import com.ticketblitz.payment.dto.TransactionResponse;
import com.ticketblitz.payment.entity.Transaction;
import com.ticketblitz.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing with idempotency and mock gateway")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction", description = "Retrieves transaction details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        log.info("GET /payments/{}", transactionId);
        
        return paymentService.getTransaction(transactionId)
                .map(TransactionResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByBooking(@PathVariable Long bookingId) {
        log.info("GET /payments/booking/{}", bookingId);
        
        List<TransactionResponse> transactions = paymentService.getTransactionsByBooking(bookingId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByUser(@PathVariable String userId) {
        log.info("GET /payments/user/{}", userId);
        
        List<TransactionResponse> transactions = paymentService.getTransactionsByUser(userId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running");
    }
}
