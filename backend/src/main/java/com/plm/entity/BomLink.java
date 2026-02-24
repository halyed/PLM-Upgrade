package com.plm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bom_links", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_revision_id", "child_revision_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BomLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_revision_id", nullable = false)
    private Revision parentRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_revision_id", nullable = false)
    private Revision childRevision;

    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;
}
