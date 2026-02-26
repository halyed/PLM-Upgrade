package com.plm.service;

import com.plm.dto.ChangeRequestRequest;
import com.plm.dto.ChangeRequestResponse;
import com.plm.entity.ChangeRequest;
import com.plm.entity.ChangeRequestStatus;
import com.plm.exception.BadRequestException;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> getAll() {
        return changeRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ChangeRequestResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ChangeRequestResponse create(ChangeRequestRequest request) {
        ChangeRequest cr = ChangeRequest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ChangeRequestStatus.OPEN)
                .linkedItemId(request.getLinkedItemId())
                .build();
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse update(Long id, ChangeRequestRequest request) {
        ChangeRequest cr = findById(id);
        if (cr.getStatus() != ChangeRequestStatus.OPEN)
            throw new BadRequestException("Only OPEN change requests can be edited");
        cr.setTitle(request.getTitle());
        cr.setDescription(request.getDescription());
        cr.setLinkedItemId(request.getLinkedItemId());
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse updateStatus(Long id, ChangeRequestStatus status) {
        ChangeRequest cr = findById(id);
        cr.setStatus(status);
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse submit(Long id) {
        ChangeRequest cr = findById(id);
        if (cr.getStatus() != ChangeRequestStatus.OPEN)
            throw new BadRequestException("Only OPEN change requests can be submitted");
        cr.setStatus(ChangeRequestStatus.IN_REVIEW);
        cr.setSubmittedBy(currentUsername());
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse approve(Long id) {
        ChangeRequest cr = findById(id);
        if (cr.getStatus() != ChangeRequestStatus.IN_REVIEW)
            throw new BadRequestException("Only IN_REVIEW change requests can be approved");
        cr.setStatus(ChangeRequestStatus.APPROVED);
        cr.setReviewedBy(currentUsername());
        cr.setReviewedAt(LocalDateTime.now());
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse reject(Long id) {
        ChangeRequest cr = findById(id);
        if (cr.getStatus() != ChangeRequestStatus.IN_REVIEW)
            throw new BadRequestException("Only IN_REVIEW change requests can be rejected");
        cr.setStatus(ChangeRequestStatus.REJECTED);
        cr.setReviewedBy(currentUsername());
        cr.setReviewedAt(LocalDateTime.now());
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public void delete(Long id) {
        changeRequestRepository.delete(findById(id));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    private ChangeRequest findById(Long id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Change request not found: " + id));
    }

    private ChangeRequestResponse toResponse(ChangeRequest cr) {
        ChangeRequestResponse resp = new ChangeRequestResponse();
        resp.setId(cr.getId());
        resp.setTitle(cr.getTitle());
        resp.setDescription(cr.getDescription());
        resp.setStatus(cr.getStatus());
        resp.setLinkedItemId(cr.getLinkedItemId());
        resp.setSubmittedBy(cr.getSubmittedBy());
        resp.setReviewedBy(cr.getReviewedBy());
        resp.setReviewedAt(cr.getReviewedAt());
        resp.setCreatedAt(cr.getCreatedAt());
        resp.setUpdatedAt(cr.getUpdatedAt());
        return resp;
    }
}
