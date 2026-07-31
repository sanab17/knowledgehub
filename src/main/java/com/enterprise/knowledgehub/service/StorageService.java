package com.enterprise.knowledgehub.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Interface for file storage operations.
 * Allows switching from local file storage to cloud storage (e.g. S3) easily.
 */
public interface StorageService {

    /**
     * Initializes the storage service (e.g., creates local upload directory if missing).
     */
    void init();

    /**
     * Stores an uploaded file and returns its generated unique filename.
     * @param file the file to store
     * @return the unique filename stored on disk
     */
    String store(MultipartFile file);

    /**
     * Loads a file path by filename.
     */
    Path load(String filename);

    /**
     * Loads a file as a spring Resource for downloading.
     */
    Resource loadAsResource(String filename);

    /**
     * Deletes a file from storage.
     */
    void delete(String filename);
}
