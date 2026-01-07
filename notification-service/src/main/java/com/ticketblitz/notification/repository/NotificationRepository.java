package com.ticketblitz.notification.repository;

import com.ticketblitz.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByBookingId(Long bookingId);
    
    List<Notification> findByUserId(String userId);
    
    List<Notification> findByStatus(Notification.NotificationStatus status);
}
