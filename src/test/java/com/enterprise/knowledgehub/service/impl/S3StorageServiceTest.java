package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.exception.Exceptions.StorageException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Resource s3Resource;

    private S3StorageService s3StorageService;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService(s3Template, bucketName);
    }

    @Test
    void init_BucketDoesNotExist_CreatesBucket() {
        when(s3Template.bucketExists(bucketName)).thenReturn(false);

        s3StorageService.init();

        verify(s3Template, times(1)).createBucket(bucketName);
    }

    @Test
    void init_BucketExists_DoesNotCreateBucket() {
        when(s3Template.bucketExists(bucketName)).thenReturn(true);

        s3StorageService.init();

        verify(s3Template, never()).createBucket(anyString());
    }

    @Test
    void store_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cloud-test.pdf", "application/pdf", "cloud content".getBytes());

        String savedName = s3StorageService.store(file);

        assertNotNull(savedName);
        assertTrue(savedName.endsWith(".pdf"));
        verify(s3Template, times(1)).upload(eq(bucketName), eq(savedName), any(InputStream.class));
    }

    @Test
    void store_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(StorageException.class, () -> s3StorageService.store(file));
    }

    @Test
    void load_Success() throws IOException {
        String filename = "existing.pdf";
        ByteArrayInputStream is = new ByteArrayInputStream("downloaded".getBytes());
        when(s3Resource.getInputStream()).thenReturn(is);
        when(s3Template.download(bucketName, filename)).thenReturn(s3Resource);

        Path tempPath = s3StorageService.load(filename);

        assertNotNull(tempPath);
        assertTrue(Files.exists(tempPath));
        assertEquals("downloaded", Files.readString(tempPath));
        Files.deleteIfExists(tempPath);
    }

    @Test
    void loadAsResource_Success() {
        String filename = "resource.pdf";
        when(s3Resource.exists()).thenReturn(true);
        when(s3Template.download(bucketName, filename)).thenReturn(s3Resource);

        Resource result = s3StorageService.loadAsResource(filename);

        assertNotNull(result);
        assertEquals(s3Resource, result);
    }

    @Test
    void loadAsResource_DoesNotExist_ThrowsException() {
        String filename = "nonexistent.pdf";
        when(s3Resource.exists()).thenReturn(false);
        when(s3Template.download(bucketName, filename)).thenReturn(s3Resource);

        assertThrows(StorageException.class, () -> s3StorageService.loadAsResource(filename));
    }

    @Test
    void delete_Success() {
        String filename = "delete.pdf";

        s3StorageService.delete(filename);

        verify(s3Template, times(1)).deleteObject(bucketName, filename);
    }
}
