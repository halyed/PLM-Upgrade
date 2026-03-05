package com.plm.service;

import com.plm.config.RabbitMqConfig;
import com.plm.dto.ConversionMessage;
import com.plm.dto.DocumentResponse;
import com.plm.entity.ConversionStatus;
import com.plm.entity.Document;
import com.plm.entity.Revision;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.DocumentRepository;
import com.plm.repository.RevisionRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RevisionRepository revisionRepository;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${minio.bucket.raw}")
    private String rawBucket;

    @Value("${minio.bucket.gltf}")
    private String gltfBucket;

    private static final Set<String> CONVERTIBLE = Set.of("STEP", "STP");

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByRevision(Long revisionId) {
        ensureRevisionExists(revisionId);
        return documentRepository.findByRevisionId(revisionId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public DocumentResponse uploadDocument(Long revisionId, MultipartFile file) {
        Revision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision not found: " + revisionId));

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toUpperCase()
                : "UNKNOWN";
        String objectName = "revisions/" + revisionId + "/" + UUID.randomUUID() + "_" + originalFilename;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            ensureBucketExists(rawBucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(rawBucket)
                    .object(objectName)
                    .stream(new java.io.ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }

        Document document = Document.builder()
                .revision(revision)
                .fileName(originalFilename)
                .filePath(rawBucket + "/" + objectName)
                .fileType(extension)
                .build();
        document = documentRepository.save(document);

        // Publish async conversion job for STEP/STP files
        if (CONVERTIBLE.contains(extension)) {
            document.setConversionStatus(ConversionStatus.PENDING);
            document = documentRepository.save(document);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.CONVERSION_EXCHANGE,
                    RabbitMqConfig.CONVERSION_KEY,
                    new ConversionMessage(document.getId(), revisionId, document.getFilePath(), originalFilename)
            );
            log.info("Queued STEP→GLB conversion for document {}", document.getId());
        }

        return toResponse(document);
    }

    @Transactional
    public void deleteDocument(Long id) {
        Document document = findById(id);
        deleteFromMinio(document.getFilePath());
        if (document.getGltfPath() != null) {
            deleteFromMinio(document.getGltfPath());
        }
        documentRepository.delete(document);
    }

    public String getDownloadUrl(Long id) {
        findById(id); // validate exists
        return "/api/documents/" + id + "/file";
    }

    public InputStream streamFile(Long id) {
        Document document = findById(id);
        // Serve the converted GLB if available, otherwise serve the raw file
        String path = document.getGltfPath() != null ? document.getGltfPath() : document.getFilePath();
        return fetchFromMinio(path);
    }

    public String getFileName(Long id) {
        Document document = findById(id);
        if (document.getGltfPath() != null) {
            return document.getFileName().replaceAll("(?i)\\.(step|stp)$", ".glb");
        }
        return document.getFileName();
    }

    public InputStream streamRawFile(Long id) {
        Document document = findById(id);
        return fetchFromMinio(document.getFilePath());
    }

    private InputStream fetchFromMinio(String fullPath) {
        try {
            String[] parts = fullPath.split("/", 2);
            if (parts.length != 2) throw new com.plm.exception.BadRequestException("Invalid file path");
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(parts[0])
                    .object(parts[1])
                    .build());
        } catch (Exception e) {
            log.error("Failed to stream file from MinIO: {}", fullPath, e);
            throw new RuntimeException("Could not stream file: " + e.getMessage(), e);
        }
    }

    private void deleteFromMinio(String fullPath) {
        try {
            String[] parts = fullPath.split("/", 2);
            if (parts.length == 2) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(parts[0])
                        .object(parts[1])
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete from MinIO: {}: {}", fullPath, e.getMessage());
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private void ensureRevisionExists(Long revisionId) {
        if (!revisionRepository.existsById(revisionId)) {
            throw new ResourceNotFoundException("Revision not found: " + revisionId);
        }
    }

    private Document findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private DocumentResponse toResponse(Document d) {
        DocumentResponse resp = new DocumentResponse();
        resp.setId(d.getId());
        resp.setRevisionId(d.getRevision().getId());
        resp.setFileName(d.getFileName());
        resp.setFilePath(d.getFilePath());
        resp.setFileType(d.getFileType());
        resp.setGltfPath(d.getGltfPath());
        resp.setConversionStatus(d.getConversionStatus() != null ? d.getConversionStatus().name() : "N_A");
        resp.setUploadedAt(d.getUploadedAt());
        return resp;
    }
}
