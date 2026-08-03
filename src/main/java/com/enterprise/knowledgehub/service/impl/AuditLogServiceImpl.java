package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.model.AuditLog;
import com.enterprise.knowledgehub.repository.AuditLogRepository;
import com.enterprise.knowledgehub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for writing and reading system audit trail logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(String action, Long documentId, String documentTitle, String username, String details) {
        log.info("Auditing action '{}' on document '{}' (ID: {}) by user '{}'", action, documentTitle, documentId, username);
        
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .documentId(documentId)
                .documentTitle(documentTitle)
                .username(username)
                .details(details)
                .build();
                
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllLogs(String username, Pageable pageable) {
        if (username != null && !username.trim().isEmpty()) {
            return auditLogRepository.findByUsernameContainingIgnoreCase(username.trim(), pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop10ByOrderByTimestampDesc();
    }
}
