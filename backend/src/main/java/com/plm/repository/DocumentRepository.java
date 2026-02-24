package com.plm.repository;

import com.plm.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByRevisionId(Long revisionId);
    List<Document> findByRevisionIdAndFileType(Long revisionId, String fileType);
}
