package com.ticketblitz.notification.service;

import com.ticketblitz.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String subject, String templateName, Context context) 
            throws MessagingException {
        log.info("Sending email to: {}, subject: {}", to, subject);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom("noreply@ticketblitz.com");
        
        String htmlContent = templateEngine.process(templateName, context);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        log.info("Email sent successfully to: {}", to);
    }

    public Context createContext(Notification notification, String eventName, String eventDate) {
        Context context = new Context();
        context.setVariable("bookingId", notification.getBookingId());
        context.setVariable("userId", notification.getUserId());
        context.setVariable("eventName", eventName);
        context.setVariable("eventDate", eventDate);
        return context;
    }
}
