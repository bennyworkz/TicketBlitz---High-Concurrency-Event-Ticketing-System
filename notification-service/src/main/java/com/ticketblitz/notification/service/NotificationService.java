package com.ticketblitz.notification.service;

import com.ticketblitz.notification.entity.Notification;
import com.ticketblitz.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public Notification sendBookingConfirmation(Long bookingId, String userId, String email, 
                                                 String eventName, String eventDate) {
        log.info("Sending booking confirmation: bookingId={}, email={}", bookingId, email);
        
        // Create notification record
        Notification notification = Notification.builder()
                .bookingId(bookingId)
                .userId(userId)
                .email(email)
                .type(Notification.NotificationType.BOOKING_CONFIRMED)
                .status(Notification.NotificationStatus.PENDING)
                .content(String.format("Booking confirmed for %s on %s", eventName, eventDate))
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send email
        try {
            Context context = emailService.createContext(notification, eventName, eventDate);
            emailService.sendEmail(email, "Booking Confirmed - TicketBlitz", 
                                  "booking-confirmed", context);
            
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Booking confirmation sent successfully: bookingId={}", bookingId);
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
            log.error("Failed to send booking confirmation: bookingId={}", bookingId, e);
        }
        
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendPaymentSuccess(Long bookingId, String userId, String email, 
                                           String amount) {
        log.info("Sending payment success notification: bookingId={}, email={}", bookingId, email);
        
        Notification notification = Notification.builder()
                .bookingId(bookingId)
                .userId(userId)
                .email(email)
                .type(Notification.NotificationType.PAYMENT_SUCCESS)
                .status(Notification.NotificationStatus.PENDING)
                .content(String.format("Payment of %s successful", amount))
                .build();
        
        notification = notificationRepository.save(notification);
        
        try {
            Context context = new Context();
            context.setVariable("bookingId", bookingId);
            context.setVariable("amount", amount);
            
            emailService.sendEmail(email, "Payment Successful - TicketBlitz", 
                                  "payment-success", context);
            
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Payment success notification sent: bookingId={}", bookingId);
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
            log.error("Failed to send payment success notification: bookingId={}", bookingId, e);
        }
        
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification sendPaymentFailed(Long bookingId, String userId, String email, 
                                          String reason) {
        log.info("Sending payment failed notification: bookingId={}, email={}", bookingId, email);
        
        Notification notification = Notification.builder()
                .bookingId(bookingId)
                .userId(userId)
                .email(email)
                .type(Notification.NotificationType.PAYMENT_FAILED)
                .status(Notification.NotificationStatus.PENDING)
                .content(String.format("Payment failed: %s", reason))
                .build();
        
        notification = notificationRepository.save(notification);
        
        try {
            Context context = new Context();
            context.setVariable("bookingId", bookingId);
            context.setVariable("reason", reason);
            
            emailService.sendEmail(email, "Payment Failed - TicketBlitz", 
                                  "payment-failed", context);
            
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Payment failed notification sent: bookingId={}", bookingId);
        } catch (Exception e) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());
            log.error("Failed to send payment failed notification: bookingId={}", bookingId, e);
        }
        
        return notificationRepository.save(notification);
    }
}
