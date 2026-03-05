package com.plm.controller;

import com.plm.dto.RevisionRequest;
import com.plm.dto.RevisionResponse;
import com.plm.entity.RevisionStatus;
import com.plm.service.RevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping("/items/{itemId}/revisions")
    public ResponseEntity<List<RevisionResponse>> getRevisionsByItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(revisionService.getRevisionsByItem(itemId));
    }

    @GetMapping("/revisions/{id}")
    public ResponseEntity<RevisionResponse> getRevision(@PathVariable Long id) {
        return ResponseEntity.ok(revisionService.getRevision(id));
    }

    @PostMapping("/items/{itemId}/revisions")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<RevisionResponse> createRevision(@PathVariable Long itemId,
                                                            @Valid @RequestBody RevisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(revisionService.createRevision(itemId, request));
    }

    @PostMapping("/items/{itemId}/revisions/next")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<RevisionResponse> nextRevision(@PathVariable Long itemId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(revisionService.nextRevision(itemId));
    }

    @PatchMapping("/revisions/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<RevisionResponse> updateStatus(@PathVariable Long id,
                                                          @RequestParam RevisionStatus status) {
        return ResponseEntity.ok(revisionService.updateRevisionStatus(id, status));
    }
}
