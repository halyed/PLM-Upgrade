package com.plm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private Long id;
    private Long revisionId;
    private String fileName;
    private String filePath;
    private String fileType;
    private String gltfPath;
    private LocalDateTime uploadedAt;
}
