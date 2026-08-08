package com.enterprise.knowledgehub.service;

import com.enterprise.knowledgehub.dto.DocumentResponseDto;
import com.enterprise.knowledgehub.dto.DocumentUploadDto;
import com.enterprise.knowledgehub.model.Department;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing documents (uploading, downloading, searching, deleting).
 */
public interface DocumentService {

    /**
     * Uploads and stores a document.
     * @param uploadDto file and meta details
     * @param username username of the owner uploading the document
     * @return the uploaded document response DTO
     */
    DocumentResponseDto uploadDocument(DocumentUploadDto uploadDto, String username);

    /**
     * Searches documents by optional title, filename, and department with pagination & sorting.
     */
    Page<DocumentResponseDto> searchDocuments(String title, String filename, Department department, Pageable pageable);

    /**
     * Retrieves document metadata by ID.
     */
    DocumentResponseDto getDocumentById(Long id);

    /**
     * Loads the physical document file resource for downloading.
     * @param id document ID
     * @param username logged in username
     */
    Resource downloadDocument(Long id, String username);

    /**
     * Deletes a document both from storage and the database.
     * Enforces ownership: only owners or ADMINs can delete.
     * @param id document ID
     * @param username logged in username
     */
    void deleteDocument(Long id, String username);

    /**
     * Counts all documents in the system.
     */
    long getTotalDocumentCount();

    /**
     * Counts documents owned by the specified username.
     */
    long getMyDocumentCount(String username);

    /**
     * Returns the 5 most recently uploaded documents.
     */
    List<DocumentResponseDto> getRecentDocuments();

    /**
     * Compiles a breakdown of document count by department.
     */
    Map<String, Long> getDepartmentStats();
}
