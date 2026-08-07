package com.enterprise.knowledgehub.service;

import com.enterprise.knowledgehub.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service interface for tracking audit events (uploads, downloads, deletes).
 */
public interface AuditLogService {
    
    /**
     * Records a new audit log entry.
     */
    void log(String action, Long documentId, String documentTitle, String username, String details);

    /**
     * Records a new audit log entry with success/failure status.
     */
    void log(String action, Long documentId, String documentTitle, String username, String details, String result);

    /**
     * Retrieves all audit logs with pagination and search term filtering.
     */
    Page<AuditLog> getAllLogs(String username, Pageable pageable);

    /**
     * Retrieves the 10 most recent audit logs.
     */
    List<AuditLog> getRecentLogs();
}
