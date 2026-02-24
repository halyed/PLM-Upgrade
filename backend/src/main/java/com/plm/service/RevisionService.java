package com.plm.service;

import com.plm.dto.RevisionRequest;
import com.plm.dto.RevisionResponse;
import com.plm.entity.Item;
import com.plm.entity.Revision;
import com.plm.entity.RevisionStatus;
import com.plm.exception.BadRequestException;
import com.plm.exception.ConflictException;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ItemRepository;
import com.plm.repository.RevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionRepository revisionRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<RevisionResponse> getRevisionsByItem(Long itemId) {
        ensureItemExists(itemId);
        return revisionRepository.findByItemIdOrderByRevisionCodeAsc(itemId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RevisionResponse getRevision(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public RevisionResponse createRevision(Long itemId, RevisionRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
        if (revisionRepository.findByItemIdAndRevisionCode(itemId, request.getRevisionCode()).isPresent()) {
            throw new ConflictException("Revision " + request.getRevisionCode() + " already exists for item " + itemId);
        }
        Revision revision = Revision.builder()
                .item(item)
                .revisionCode(request.getRevisionCode())
                .status(request.getStatus() != null ? request.getStatus() : RevisionStatus.IN_WORK)
                .build();
        return toResponse(revisionRepository.save(revision));
    }

    @Transactional
    public RevisionResponse nextRevision(Long itemId) {
        ensureItemExists(itemId);
        List<Revision> revisions = revisionRepository.findByItemIdOrderByRevisionCodeAsc(itemId);
        String nextCode = revisions.isEmpty() ? "A" : nextRevisionCode(
                revisions.get(revisions.size() - 1).getRevisionCode());
        Revision revision = Revision.builder()
                .item(itemRepository.getReferenceById(itemId))
                .revisionCode(nextCode)
                .status(RevisionStatus.IN_WORK)
                .build();
        return toResponse(revisionRepository.save(revision));
    }

    @Transactional
    public RevisionResponse updateRevisionStatus(Long id, RevisionStatus status) {
        Revision revision = findById(id);
        revision.setStatus(status);
        return toResponse(revisionRepository.save(revision));
    }

    private String nextRevisionCode(String current) {
        if (current.length() == 1) {
            char c = current.charAt(0);
            if (c == 'Z') throw new BadRequestException("Maximum revision code Z reached");
            return String.valueOf((char) (c + 1));
        }
        throw new BadRequestException("Unsupported multi-character revision codes");
    }

    private void ensureItemExists(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found: " + itemId);
        }
    }

    private Revision findById(Long id) {
        return revisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revision not found: " + id));
    }

    private RevisionResponse toResponse(Revision r) {
        RevisionResponse resp = new RevisionResponse();
        resp.setId(r.getId());
        resp.setItemId(r.getItem().getId());
        resp.setItemNumber(r.getItem().getItemNumber());
        resp.setRevisionCode(r.getRevisionCode());
        resp.setStatus(r.getStatus());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
