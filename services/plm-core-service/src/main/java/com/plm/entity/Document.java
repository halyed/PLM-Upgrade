package com.plm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_id", nullable = false)
    private Revision revision;

    @Column(name = "file_name", nullable = false, length = 255)
    @NotBlank
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1024)
    @NotBlank
    private String filePath;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "gltf_path", length = 1024)
    private String gltfPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_status", length = 20)
    @Builder.Default
    private ConversionStatus conversionStatus = ConversionStatus.N_A;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
