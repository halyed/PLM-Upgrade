package com.plm.notification.repository;

import com.plm.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientInOrderByCreatedAtDesc(
            java.util.List<String> recipients, Pageable pageable);

    long countByRecipientInAndReadFalse(java.util.List<String> recipients);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id")
    void markRead(Long id);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient IN :recipients")
    void markAllRead(java.util.List<String> recipients);
}
