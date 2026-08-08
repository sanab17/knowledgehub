package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.model.AuditLog;
import com.enterprise.knowledgehub.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void log_Success() {
        // Act
        auditLogService.log("UPLOAD", 1L, "Document.pdf", "testuser", "Uploaded file");

        // Assert
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getAllLogs_WithUsername_FiltersByUsername() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findByUsernameContainingIgnoreCase("testuser", pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllLogs("testuser", pageable);

        // Assert
        assertNotNull(result);
        verify(auditLogRepository, times(1)).findByUsernameContainingIgnoreCase("testuser", pageable);
        verify(auditLogRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllLogs_NullUsername_FindsAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllLogs(null, pageable);

        // Assert
        assertNotNull(result);
        verify(auditLogRepository, times(1)).findAll(pageable);
        verify(auditLogRepository, never()).findByUsernameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void getAllLogs_EmptyUsername_FindsAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllLogs("", pageable);

        // Assert
        assertNotNull(result);
        verify(auditLogRepository, times(1)).findAll(pageable);
        verify(auditLogRepository, never()).findByUsernameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void getAllLogs_WhitespaceUsername_FindsAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllLogs("   ", pageable);

        // Assert
        assertNotNull(result);
        verify(auditLogRepository, times(1)).findAll(pageable);
        verify(auditLogRepository, never()).findByUsernameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void getRecentLogs_Success() {
        // Arrange
        List<AuditLog> logs = Collections.emptyList();
        when(auditLogRepository.findTop10ByOrderByTimestampDesc()).thenReturn(logs);

        // Act
        List<AuditLog> result = auditLogService.getRecentLogs();

        // Assert
        assertNotNull(result);
        verify(auditLogRepository, times(1)).findTop10ByOrderByTimestampDesc();
    }

    @Test
    void log_WithHttpRequestContext_SavesRequestDetails() {
        // Arrange
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.10, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0.0");
        
        org.springframework.web.context.request.ServletRequestAttributes attributes = 
                new org.springframework.web.context.request.ServletRequestAttributes(request);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attributes);

        try {
            // Act
            auditLogService.log("UPLOAD", 1L, "Document.pdf", "testuser", "Uploaded file", "SUCCESS");

            // Assert
            org.mockito.ArgumentCaptor<AuditLog> captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, times(1)).save(captor.capture());
            AuditLog savedLog = captor.getValue();
            
            assertNotNull(savedLog);
            assertEquals("192.168.1.10", savedLog.getIpAddress());
            assertEquals("Mozilla/5.0 Chrome/120.0.0", savedLog.getUserAgent());
            assertEquals("SUCCESS", savedLog.getResult());
            assertEquals("Chrome", savedLog.getBrowserName());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }
}
