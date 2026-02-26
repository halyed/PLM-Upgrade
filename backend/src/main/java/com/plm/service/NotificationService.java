package com.plm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyConversionUpdate(Long documentId, String status, String gltfPath) {
        messagingTemplate.convertAndSend("/topic/conversions", Map.of(
                "documentId", documentId,
                "status", status,
                "gltfPath", gltfPath != null ? gltfPath : ""
        ));
    }
}
