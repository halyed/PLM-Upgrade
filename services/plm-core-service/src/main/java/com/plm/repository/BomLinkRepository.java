package com.plm.repository;

import com.plm.entity.BomLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomLinkRepository extends JpaRepository<BomLink, Long> {
    List<BomLink> findByParentRevisionId(Long parentRevisionId);
    List<BomLink> findByChildRevisionId(Long childRevisionId);
    boolean existsByParentRevisionIdAndChildRevisionId(Long parentRevisionId, Long childRevisionId);
    void deleteByParentRevisionIdAndChildRevisionId(Long parentRevisionId, Long childRevisionId);
}
