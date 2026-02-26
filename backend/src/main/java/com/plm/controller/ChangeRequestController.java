package com.plm.controller;

import com.plm.dto.ChangeRequestRequest;
import com.plm.dto.ChangeRequestResponse;
import com.plm.entity.ChangeRequestStatus;
import com.plm.service.ChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @GetMapping
    public ResponseEntity<List<ChangeRequestResponse>> getAll() {
        return ResponseEntity.ok(changeRequestService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(changeRequestService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ChangeRequestResponse> create(@Valid @RequestBody ChangeRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(changeRequestService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ChangeRequestResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody ChangeRequestRequest request) {
        return ResponseEntity.ok(changeRequestService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ChangeRequestResponse> updateStatus(@PathVariable Long id,
                                                               @RequestParam ChangeRequestStatus status) {
        return ResponseEntity.ok(changeRequestService.updateStatus(id, status));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ChangeRequestResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(changeRequestService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChangeRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(changeRequestService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChangeRequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(changeRequestService.reject(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        changeRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
