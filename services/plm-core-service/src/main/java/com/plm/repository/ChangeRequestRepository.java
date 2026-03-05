package com.plm.repository;

import com.plm.entity.ChangeRequest;
import com.plm.entity.ChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
    List<ChangeRequest> findByStatus(ChangeRequestStatus status);
    List<ChangeRequest> findByTitleContainingIgnoreCase(String title);
}
