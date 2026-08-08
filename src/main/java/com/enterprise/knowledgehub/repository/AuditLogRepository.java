package com.enterprise.knowledgehub.repository;

import com.enterprise.knowledgehub.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing AuditLog entity database operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    List<AuditLog> findTop10ByOrderByTimestampDesc();
}
