package com.plm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "revisions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"item_id", "revision_code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "revision_code", nullable = false, length = 10)
    @NotBlank
    private String revisionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RevisionStatus status = RevisionStatus.IN_WORK;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "parentRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BomLink> bomChildren = new ArrayList<>();

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
}
