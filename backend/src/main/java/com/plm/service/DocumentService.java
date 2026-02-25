package com.plm.service;

import com.plm.dto.DocumentResponse;
import com.plm.entity.Document;
import com.plm.entity.Revision;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.DocumentRepository;
import com.plm.repository.RevisionRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RevisionRepository revisionRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket.raw}")
    private String rawBucket;

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

        try {
            ensureBucketExists(rawBucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(rawBucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
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
        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public void deleteDocument(Long id) {
        Document document = findById(id);
        try {
            String[] parts = document.getFilePath().split("/", 2);
            if (parts.length == 2) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(parts[0])
                        .object(parts[1])
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", e.getMessage());
        }
        documentRepository.delete(document);
    }

    public String getDownloadUrl(Long id) {
        Document document = findById(id);
        try {
            String[] parts = document.getFilePath().split("/", 2);
            if (parts.length != 2) throw new com.plm.exception.BadRequestException("Invalid file path");
            return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(parts[0])
                    .object(parts[1])
                    .expiry(1, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new RuntimeException("Could not generate download URL: " + e.getMessage(), e);
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
        resp.setUploadedAt(d.getUploadedAt());
        return resp;
    }
}
