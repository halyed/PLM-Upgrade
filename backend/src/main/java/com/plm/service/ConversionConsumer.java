package com.plm.service;

import com.plm.config.RabbitMqConfig;
import com.plm.dto.ConversionMessage;
import com.plm.entity.ConversionStatus;
import com.plm.entity.Document;
import com.plm.repository.DocumentRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionConsumer {

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    @Value("${conversion.service.url}")
    private String conversionServiceUrl;

    @Value("${minio.bucket.gltf}")
    private String gltfBucket;

    @RabbitListener(queues = RabbitMqConfig.CONVERSION_QUEUE)
    @Transactional
    public void handleConversion(ConversionMessage msg) {
        Document document = documentRepository.findById(msg.documentId()).orElse(null);
        if (document == null) return;

        log.info("Processing conversion for document {}", msg.documentId());
        document.setConversionStatus(ConversionStatus.CONVERTING);
        documentRepository.save(document);
        notificationService.notifyConversionUpdate(msg.documentId(), "CONVERTING", null);

        try {
            // Fetch raw file from MinIO
            byte[] stepBytes;
            try {
                String[] parts = msg.filePath().split("/", 2);
                var stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(parts[0]).object(parts[1]).build());
                stepBytes = stream.readAllBytes();
                stream.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch raw file: " + e.getMessage(), e);
            }

            // Call conversion service
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(stepBytes) {
                @Override public String getFilename() { return msg.originalFilename(); }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    conversionServiceUrl + "/convert/step-to-glb",
                    new HttpEntity<>(body, headers), byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
                throw new RuntimeException("Conversion service returned: " + response.getStatusCode());

            // Store GLB in MinIO
            byte[] glbBytes = response.getBody();
            String glbName = "revisions/" + msg.revisionId() + "/" + UUID.randomUUID()
                    + "_" + msg.originalFilename().replaceAll("(?i)\\.(step|stp)$", ".glb");
            ensureBucketExists(gltfBucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(gltfBucket).object(glbName)
                    .stream(new java.io.ByteArrayInputStream(glbBytes), glbBytes.length, -1)
                    .contentType("model/gltf-binary").build());

            document.setGltfPath(gltfBucket + "/" + glbName);
            document.setConversionStatus(ConversionStatus.DONE);
            documentRepository.save(document);
            log.info("Conversion DONE for document {}", msg.documentId());
            notificationService.notifyConversionUpdate(msg.documentId(), "DONE", document.getGltfPath());

        } catch (Exception e) {
            log.error("Conversion FAILED for document {}: {}", msg.documentId(), e.getMessage());
            document.setConversionStatus(ConversionStatus.FAILED);
            documentRepository.save(document);
            notificationService.notifyConversionUpdate(msg.documentId(), "FAILED", null);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }
}
