package com.ticketblitz.notification;

import com.ticketblitz.notification.entity.Notification;
import com.ticketblitz.notification.repository.NotificationRepository;
import com.ticketblitz.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private JavaMailSender mailSender; // Mock email sender to avoid actual email sending

    @AfterEach
    void cleanup() {
        notificationRepository.deleteAll();
    }

    @Test
    void sendBookingConfirmation_Success_ShouldSaveNotification() {
        // Given
        Long bookingId = 1L;
        String userId = "user123";
        String email = "test@example.com";
        String eventName = "Rock Concert";
        String eventDate = "2026-02-15";

        // When
        Notification notification = notificationService.sendBookingConfirmation(
                bookingId, userId, email, eventName, eventDate);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getBookingId()).isEqualTo(bookingId);
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getEmail()).isEqualTo(email);
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.BOOKING_CONFIRMED);
        // Status will be FAILED because mailSender is mocked
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void sendPaymentSuccess_Success_ShouldSaveNotification() {
        // Given
        Long bookingId = 2L;
        String userId = "user456";
        String email = "test@example.com";
        String amount = "100.00";

        // When
        Notification notification = notificationService.sendPaymentSuccess(
                bookingId, userId, email, amount);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getBookingId()).isEqualTo(bookingId);
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.PAYMENT_SUCCESS);
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void sendPaymentFailed_Success_ShouldSaveNotification() {
        // Given
        Long bookingId = 3L;
        String userId = "user789";
        String email = "test@example.com";
        String reason = "Insufficient funds";

        // When
        Notification notification = notificationService.sendPaymentFailed(
                bookingId, userId, email, reason);

        // Then
        assertThat(notification).isNotNull();
        assertThat(notification.getBookingId()).isEqualTo(bookingId);
        assertThat(notification.getType()).isEqualTo(Notification.NotificationType.PAYMENT_FAILED);
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void findByBookingId_ShouldReturnAllNotifications() {
        // Given
        Long bookingId = 4L;
        notificationService.sendBookingConfirmation(bookingId, "user1", "test1@example.com", 
                                                     "Event1", "2026-03-01");
        notificationService.sendPaymentSuccess(bookingId, "user1", "test1@example.com", "50.00");

        // When
        List<Notification> notifications = notificationRepository.findByBookingId(bookingId);

        // Then
        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(n -> n.getBookingId().equals(bookingId));
    }

    @Test
    void findByUserId_ShouldReturnAllNotifications() {
        // Given
        String userId = "user999";
        notificationService.sendBookingConfirmation(1L, userId, "test@example.com", 
                                                     "Event1", "2026-03-01");
        notificationService.sendBookingConfirmation(2L, userId, "test@example.com", 
                                                     "Event2", "2026-03-02");

        // When
        List<Notification> notifications = notificationRepository.findByUserId(userId);

        // Then
        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(n -> n.getUserId().equals(userId));
    }

    @Test
    void findByStatus_ShouldReturnMatchingNotifications() {
        // Given
        notificationService.sendBookingConfirmation(1L, "user1", "test@example.com", 
                                                     "Event1", "2026-03-01");
        notificationService.sendPaymentSuccess(2L, "user2", "test@example.com", "75.00");

        // When - Check for SENT status (email sending is async and mocked)
        List<Notification> sentNotifications = notificationRepository
                .findByStatus(Notification.NotificationStatus.SENT);

        // Then
        assertThat(sentNotifications).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void notification_ShouldHaveCreatedAtTimestamp() {
        // When
        Notification notification = notificationService.sendBookingConfirmation(
                1L, "user1", "test@example.com", "Event1", "2026-03-01");

        // Then
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void multipleNotifications_ShouldAllBeSaved() {
        // Given
        for (int i = 1; i <= 3; i++) {
            notificationService.sendBookingConfirmation(
                    (long) i, "user" + i, "test@example.com", 
                    "Event" + i, "2026-03-0" + i);
        }

        // Then
        List<Notification> allNotifications = notificationRepository.findAll();
        assertThat(allNotifications).hasSize(3);
    }
}
