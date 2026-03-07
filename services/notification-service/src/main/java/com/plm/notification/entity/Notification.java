package com.plm.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;        // ITEM_CREATED, LIFECYCLE_CHANGED, WORKFLOW_APPROVED, etc.

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    private String entityType;  // item | revision | change-request
    private String entityId;
    private String recipient;   // username or "ALL"

    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    private Instant createdAt;
}
