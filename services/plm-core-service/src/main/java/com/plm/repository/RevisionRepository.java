package com.plm.repository;

import com.plm.entity.Revision;
import com.plm.entity.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionRepository extends JpaRepository<Revision, Long> {
    List<Revision> findByItemId(Long itemId);
    Optional<Revision> findByItemIdAndRevisionCode(Long itemId, String revisionCode);
    List<Revision> findByItemIdOrderByRevisionCodeAsc(Long itemId);
    List<Revision> findByStatus(RevisionStatus status);
}
