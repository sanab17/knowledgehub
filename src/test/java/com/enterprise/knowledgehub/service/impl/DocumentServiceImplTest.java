package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.dto.DocumentResponseDto;
import com.enterprise.knowledgehub.dto.DocumentUploadDto;
import com.enterprise.knowledgehub.exception.Exceptions.InvalidFileException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import com.enterprise.knowledgehub.exception.Exceptions.UnauthorizedException;
import com.enterprise.knowledgehub.model.Department;
import com.enterprise.knowledgehub.model.Document;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.repository.DocumentRepository;
import com.enterprise.knowledgehub.repository.UserRepository;
import com.enterprise.knowledgehub.service.StorageService;
import com.enterprise.knowledgehub.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private User testUser;
    private User adminUser;
    private DocumentUploadDto uploadDto;
    private MockMultipartFile validPdfFile;

    @BeforeEach
    void setUp() {
        // Set @Value properties via ReflectionTestUtils
        ReflectionTestUtils.setField(documentService, "allowedTypes", List.of("application/pdf"));
        ReflectionTestUtils.setField(documentService, "maxSizeBytes", 10 * 1024 * 1024L);

        testUser = User.builder()
                .id(1L)
                .username("employee1")
                .role(UserRole.EMPLOYEE)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();

        validPdfFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy pdf content".getBytes());

        uploadDto = DocumentUploadDto.builder()
                .title("Annual HR Policy")
                .description("HR Guidelines")
                .department(Department.HR)
                .file(validPdfFile)
                .build();
    }

    @Test
    void uploadDocument_Success() {
        // Arrange
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(testUser));
        when(storageService.store(any())).thenReturn("unique-uuid.pdf");
        
        Document savedDoc = Document.builder()
                .id(100L)
                .title("Annual HR Policy")
                .description("HR Guidelines")
                .filename("unique-uuid.pdf")
                .fileSize(validPdfFile.getSize())
                .contentType("application/pdf")
                .department(Department.HR)
                .owner(testUser)
                .build();
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

        // Act
        DocumentResponseDto result = documentService.uploadDocument(uploadDto, "employee1");

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Annual HR Policy", result.getTitle());
        assertEquals("unique-uuid.pdf", result.getFilename());
        verify(storageService, times(1)).store(any());
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(auditLogService, times(1)).log(eq("UPLOAD"), eq(100L), eq("Annual HR Policy"), eq("employee1"), anyString());
    }

    @Test
    void uploadDocument_InvalidFileType_ThrowsException() {
        // Arrange
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "unsupported.txt", "text/plain", "plain text".getBytes());
        uploadDto.setFile(txtFile);
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(InvalidFileException.class, () -> {
            documentService.uploadDocument(uploadDto, "employee1");
        });
        verify(storageService, never()).store(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_FileExceedingMaxSize_ThrowsException() {
        // Arrange
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", new byte[11 * 1024 * 1024]); // 11MB
        uploadDto.setFile(largeFile);
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(InvalidFileException.class, () -> {
            documentService.uploadDocument(uploadDto, "employee1");
        });
        verify(storageService, never()).store(any());
    }

    @Test
    void downloadDocument_Success() {
        // Arrange
        Document doc = Document.builder()
                .id(100L)
                .title("Manual")
                .filename("manual-uuid.pdf")
                .owner(testUser)
                .build();
        Resource mockResource = mock(Resource.class);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(storageService.loadAsResource("manual-uuid.pdf")).thenReturn(mockResource);

        // Act
        Resource result = documentService.downloadDocument(100L, "employee1");

        // Assert
        assertNotNull(result);
        assertEquals(mockResource, result);
        verify(auditLogService, times(1)).log(eq("DOWNLOAD"), eq(100L), eq("Manual"), eq("employee1"), anyString());
    }

    @Test
    void deleteDocument_Success_AsOwner() {
        // Arrange
        Document doc = Document.builder()
                .id(100L)
                .title("Delete Me")
                .filename("delete-uuid.pdf")
                .owner(testUser)
                .build();
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        // Act
        documentService.deleteDocument(100L, "employee1");

        // Assert
        verify(storageService, times(1)).delete("delete-uuid.pdf");
        verify(documentRepository, times(1)).delete(doc);
        verify(auditLogService, times(1)).log(eq("DELETE"), eq(100L), eq("Delete Me"), eq("employee1"), anyString());
    }

    @Test
    void deleteDocument_Success_AsAdmin() {
        // Arrange
        Document doc = Document.builder()
                .id(100L)
                .title("Delete Me")
                .filename("delete-uuid.pdf")
                .owner(testUser) // owned by testUser
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser)); // requestor is adminUser
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        // Act
        documentService.deleteDocument(100L, "admin");

        // Assert
        verify(storageService, times(1)).delete("delete-uuid.pdf");
        verify(documentRepository, times(1)).delete(doc);
        verify(auditLogService, times(1)).log(eq("DELETE"), eq(100L), eq("Delete Me"), eq("admin"), anyString());
    }

    @Test
    void deleteDocument_UnauthorizedEmployee_ThrowsException() {
        // Arrange
        User anotherEmployee = User.builder()
                .id(99L)
                .username("another")
                .role(UserRole.EMPLOYEE)
                .build();
        Document doc = Document.builder()
                .id(100L)
                .title("Owned By TestUser")
                .owner(testUser) // owned by testUser
                .build();
        
        when(userRepository.findByUsername("another")).thenReturn(Optional.of(anotherEmployee));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            documentService.deleteDocument(100L, "another");
        });
        verify(storageService, never()).delete(anyString());
        verify(documentRepository, never()).delete(any(Document.class));
        verify(auditLogService, never()).log(any(), any(), any(), any(), any());
    }
}
