package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.exception.Exceptions.StorageException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import com.enterprise.knowledgehub.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Local file system implementation of the {@link StorageService}.
 * Manages document storage locally in a configured folder.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized local upload directory at: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not initialize storage directory: {}", rootLocation, e);
            throw new StorageException("Could not initialize storage directory", e);
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

        // Generate a secure unique name to prevent collisions and directory traversal attacks
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            if (uniqueFilename.contains("..")) {
                throw new StorageException("Cannot store file with relative path outside current directory " + uniqueFilename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Path targetLocation = this.rootLocation.resolve(uniqueFilename);
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                log.info("Stored file locally: {} as {}", originalFilename, uniqueFilename);
                return uniqueFilename;
            }
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new StorageException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                log.error("Could not read file: {}", filename);
                throw new ResourceNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            log.error("Could not read file: {} due to URL malformation", filename, e);
            throw new ResourceNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = load(filename);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("Deleted file from local storage: {}", filename);
            } else {
                log.warn("File to delete not found: {}", filename);
            }
        } catch (IOException e) {
            log.error("Failed to delete file from local storage: {}", filename, e);
            throw new StorageException("Failed to delete file: " + filename, e);
        }
    }
}
