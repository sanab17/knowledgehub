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
import org.springframework.transaction.annotation.Propagation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, Long documentId, String documentTitle, String username, String details) {
        log(action, documentId, documentTitle, username, details, "SUCCESS");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, Long documentId, String documentTitle, String username, String details, String result) {
        log.info("Auditing action '{}' (result: {}) on document '{}' (ID: {}) by user '{}'", action, result, documentTitle, documentId, username);
        
        String ipAddress = "UNKNOWN";
        String userAgent = "UNKNOWN";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Extract IP (check X-Forwarded-For header for load balancer proxy routing)
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            } else {
                int commaIndex = ipAddress.indexOf(',');
                if (commaIndex != -1) {
                    ipAddress = ipAddress.substring(0, commaIndex).trim();
                }
            }

            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 497) + "...";
            }
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .documentId(documentId)
                .documentTitle(documentTitle)
                .username(username)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result(result)
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
