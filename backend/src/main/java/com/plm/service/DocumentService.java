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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate;

    @Value("${minio.bucket.raw}")
    private String rawBucket;

    @Value("${minio.bucket.gltf}")
    private String gltfBucket;

    @Value("${conversion.service.url}")
    private String conversionServiceUrl;

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

        // Trigger STEP → GLB conversion if applicable
        if (CONVERTIBLE.contains(extension)) {
            document = convertAndStore(document, fileBytes, originalFilename, revisionId);
        }

        return toResponse(document);
    }

    private Document convertAndStore(Document document, byte[] stepBytes, String originalFilename, Long revisionId) {
        try {
            log.info("Starting STEP→GLB conversion for document {}", document.getId());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(stepBytes) {
                @Override public String getFilename() { return originalFilename; }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    conversionServiceUrl + "/convert/step-to-glb",
                    new HttpEntity<>(body, headers),
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] glbBytes = response.getBody();
                String glbName = "revisions/" + revisionId + "/" + UUID.randomUUID() + "_"
                        + originalFilename.replaceAll("(?i)\\.(step|stp)$", ".glb");

                ensureBucketExists(gltfBucket);
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(gltfBucket)
                        .object(glbName)
                        .stream(new java.io.ByteArrayInputStream(glbBytes), glbBytes.length, -1)
                        .contentType("model/gltf-binary")
                        .build());

                document.setGltfPath(gltfBucket + "/" + glbName);
                document = documentRepository.save(document);
                log.info("Conversion successful for document {}, GLB stored at {}", document.getId(), document.getGltfPath());
            }
        } catch (Exception e) {
            log.warn("Conversion failed for document {}: {}", document.getId(), e.getMessage());
            // Non-fatal: document is still usable, just not viewable in 3D
        }
        return document;
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
        resp.setUploadedAt(d.getUploadedAt());
        return resp;
    }
}
