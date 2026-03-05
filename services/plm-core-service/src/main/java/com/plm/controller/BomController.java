package com.plm.controller;

import com.plm.dto.BomLinkRequest;
import com.plm.dto.BomLinkResponse;
import com.plm.service.BomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revisions/{revisionId}/bom")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @GetMapping("/children")
    public ResponseEntity<List<BomLinkResponse>> getChildren(@PathVariable Long revisionId) {
        return ResponseEntity.ok(bomService.getChildren(revisionId));
    }

    @GetMapping("/parents")
    public ResponseEntity<List<BomLinkResponse>> getParents(@PathVariable Long revisionId) {
        return ResponseEntity.ok(bomService.getParents(revisionId));
    }

    @PostMapping("/children")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<BomLinkResponse> addChild(@PathVariable Long revisionId,
                                                     @Valid @RequestBody BomLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bomService.addChild(revisionId, request));
    }

    @DeleteMapping("/children/{childRevisionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<Void> removeChild(@PathVariable Long revisionId,
                                             @PathVariable Long childRevisionId) {
        bomService.removeChild(revisionId, childRevisionId);
        return ResponseEntity.noContent().build();
    }
}
