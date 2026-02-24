package com.plm.service;

import com.plm.dto.ChangeRequestRequest;
import com.plm.dto.ChangeRequestResponse;
import com.plm.entity.ChangeRequest;
import com.plm.entity.ChangeRequestStatus;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .status(request.getStatus() != null ? request.getStatus() : ChangeRequestStatus.OPEN)
                .build();
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse update(Long id, ChangeRequestRequest request) {
        ChangeRequest cr = findById(id);
        cr.setTitle(request.getTitle());
        cr.setDescription(request.getDescription());
        if (request.getStatus() != null) cr.setStatus(request.getStatus());
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public ChangeRequestResponse updateStatus(Long id, ChangeRequestStatus status) {
        ChangeRequest cr = findById(id);
        cr.setStatus(status);
        return toResponse(changeRequestRepository.save(cr));
    }

    @Transactional
    public void delete(Long id) {
        changeRequestRepository.delete(findById(id));
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
        resp.setCreatedAt(cr.getCreatedAt());
        resp.setUpdatedAt(cr.getUpdatedAt());
        return resp;
    }
}
