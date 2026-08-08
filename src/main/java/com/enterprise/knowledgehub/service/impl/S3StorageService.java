package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.exception.Exceptions.StorageException;
import com.enterprise.knowledgehub.service.StorageService;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Cloud S3 implementation of {@link StorageService}.
 * Handles storage operations using AWS S3 / MinIO via S3Template.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Template s3Template;
    private final String bucketName;

    public S3StorageService(S3Template s3Template, 
                            @Value("${app.storage.s3.bucket:knowledgehub-uploads}") String bucketName) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            if (!s3Template.bucketExists(bucketName)) {
                log.info("S3 bucket '{}' does not exist. Attempting to create...", bucketName);
                s3Template.createBucket(bucketName);
                log.info("Successfully created S3 bucket: {}", bucketName);
            } else {
                log.info("Connected to S3 bucket: {}", bucketName);
            }
        } catch (Exception e) {
            // Catch bucket creation errors gracefully (e.g. if bucket exists but bucketExists returns false,
            // or if the IAM policy doesn't allow bucket creation but the bucket already exists).
            log.warn("Could not verify/create S3 bucket '{}' during startup. Verification will continue during runtime. Error: {}", bucketName, e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = "";

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // Generate a secure unique filename for cloud storage
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            if (uniqueFilename.contains("..")) {
                throw new StorageException("Cannot store file with relative path outside current directory " + uniqueFilename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                s3Template.upload(bucketName, uniqueFilename, inputStream);
                log.info("Successfully uploaded file to S3: {} as {}", originalFilename, uniqueFilename);
                return uniqueFilename;
            }
        } catch (IOException e) {
            log.error("Failed to read file stream for {}", originalFilename, e);
            throw new StorageException("Failed to store file in S3: " + originalFilename, e);
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", originalFilename, e);
            throw new StorageException("Failed to store file in S3: " + originalFilename, e);
        }
    }

    @Override
    public Path load(String filename) {
        try {
            S3Resource s3Resource = s3Template.download(bucketName, filename);
            Path tempFile = Files.createTempFile("knowledgehub-", "-" + filename);
            try (InputStream inputStream = s3Resource.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (IOException e) {
            log.error("Failed to download S3 object to temp path: {}", filename, e);
            throw new StorageException("Failed to load file from S3: " + filename, e);
        } catch (Exception e) {
            log.error("S3 error loading file: {}", filename, e);
            throw new StorageException("Failed to load file from S3: " + filename, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            S3Resource resource = s3Template.download(bucketName, filename);
            if (resource.exists()) {
                return resource;
            } else {
                log.error("S3 object does not exist: {}", filename);
                throw new StorageException("Could not read file from S3: " + filename);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve S3 resource: {}", filename, e);
            throw new StorageException("Could not read file from S3: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            s3Template.deleteObject(bucketName, filename);
            log.info("Successfully deleted file from S3: {}", filename);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", filename, e);
            throw new StorageException("Failed to delete file from S3: " + filename, e);
        }
    }
}
