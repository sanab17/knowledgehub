package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.exception.Exceptions.StorageException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService storageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storageService = new LocalStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    void store_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        String savedName = storageService.store(file);
        
        assertNotNull(savedName);
        assertTrue(savedName.endsWith(".pdf"));
        
        Path filePath = storageService.load(savedName);
        assertTrue(Files.exists(filePath));
        assertEquals("dummy content", Files.readString(filePath));
    }

    @Test
    void store_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(StorageException.class, () -> storageService.store(file));
    }

    @Test
    void loadAsResource_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx content".getBytes());

        String savedName = storageService.store(file);
        Resource resource = storageService.loadAsResource(savedName);
        
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadAsResource_FileNotFound_ThrowsException() {
        assertThrows(ResourceNotFoundException.class, () -> storageService.loadAsResource("nonexistent.pdf"));
    }

    @Test
    void delete_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete.pdf", "application/pdf", "content".getBytes());

        String savedName = storageService.store(file);
        Path filePath = storageService.load(savedName);
        assertTrue(Files.exists(filePath));

        storageService.delete(savedName);
        assertFalse(Files.exists(filePath));
    }
}
