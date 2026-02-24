package com.plm.service;

import com.plm.dto.BomLinkRequest;
import com.plm.dto.BomLinkResponse;
import com.plm.entity.BomLink;
import com.plm.entity.Revision;
import com.plm.exception.BadRequestException;
import com.plm.exception.ConflictException;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.BomLinkRepository;
import com.plm.repository.RevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BomService {

    private final BomLinkRepository bomLinkRepository;
    private final RevisionRepository revisionRepository;

    @Transactional(readOnly = true)
    public List<BomLinkResponse> getChildren(Long parentRevisionId) {
        ensureRevisionExists(parentRevisionId);
        return bomLinkRepository.findByParentRevisionId(parentRevisionId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BomLinkResponse> getParents(Long childRevisionId) {
        ensureRevisionExists(childRevisionId);
        return bomLinkRepository.findByChildRevisionId(childRevisionId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BomLinkResponse addChild(Long parentRevisionId, BomLinkRequest request) {
        if (parentRevisionId.equals(request.getChildRevisionId())) {
            throw new BadRequestException("A revision cannot be its own BOM child");
        }
        Revision parent = findRevision(parentRevisionId);
        Revision child = findRevision(request.getChildRevisionId());
        if (bomLinkRepository.existsByParentRevisionIdAndChildRevisionId(parentRevisionId, request.getChildRevisionId())) {
            throw new ConflictException("BOM link already exists");
        }
        BomLink link = BomLink.builder()
                .parentRevision(parent)
                .childRevision(child)
                .quantity(request.getQuantity())
                .build();
        return toResponse(bomLinkRepository.save(link));
    }

    @Transactional
    public void removeChild(Long parentRevisionId, Long childRevisionId) {
        if (!bomLinkRepository.existsByParentRevisionIdAndChildRevisionId(parentRevisionId, childRevisionId)) {
            throw new ResourceNotFoundException("BOM link not found");
        }
        bomLinkRepository.deleteByParentRevisionIdAndChildRevisionId(parentRevisionId, childRevisionId);
    }

    private Revision findRevision(Long id) {
        return revisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revision not found: " + id));
    }

    private void ensureRevisionExists(Long id) {
        if (!revisionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Revision not found: " + id);
        }
    }

    private BomLinkResponse toResponse(BomLink link) {
        BomLinkResponse resp = new BomLinkResponse();
        resp.setId(link.getId());
        resp.setParentRevisionId(link.getParentRevision().getId());
        resp.setChildRevisionId(link.getChildRevision().getId());
        resp.setChildItemNumber(link.getChildRevision().getItem().getItemNumber());
        resp.setChildRevisionCode(link.getChildRevision().getRevisionCode());
        resp.setQuantity(link.getQuantity());
        return resp;
    }
}
