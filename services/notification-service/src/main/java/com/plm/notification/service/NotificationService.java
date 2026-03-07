package com.plm.notification.service;

import com.plm.notification.entity.Notification;
import com.plm.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate ws;
    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@plm.local}")
    private String emailFrom;

    public void create(String type, String title, String message, String entityType, String entityId, String recipient) {
        Notification n = Notification.builder()
                .type(type).title(title).message(message)
                .entityType(entityType).entityId(entityId)
                .recipient(recipient).build();
        n = repository.save(n);
        log.info("Notification created: [{}] {} → {}", type, title, recipient);

        // Push via WebSocket to relevant subscribers
        ws.convertAndSend("/topic/notifications/" + recipient, Map.of(
                "id", n.getId(), "type", type, "title", title, "message", message,
                "entityType", entityType != null ? entityType : "",
                "entityId", entityId != null ? entityId : ""
        ));
        if (!"ALL".equals(recipient)) {
            // Also push to ALL channel for admin dashboards
            ws.convertAndSend("/topic/notifications/ALL", Map.of(
                    "id", n.getId(), "type", type, "title", title, "message", message));
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getForUser(String username, int page, int size) {
        return repository.findByRecipientInOrderByCreatedAtDesc(
                List.of(username, "ALL"), PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return repository.countByRecipientInAndReadFalse(List.of(username, "ALL"));
    }

    @Transactional
    public void markRead(Long id) {
        repository.markRead(id);
    }

    @Transactional
    public void markAllRead(String username) {
        repository.markAllRead(List.of(username, "ALL"));
    }

    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled || to == null || to.isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(emailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
