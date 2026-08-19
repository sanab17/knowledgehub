package com.enterprise.knowledgehub.service;

import org.springframework.ai.document.Document;
import com.enterprise.knowledgehub.model.Department;
import java.util.List;

/**
 * Service to retrieve semantically matching document chunks from the vector store
 * with document-level authorization rules applied.
 */
public interface RagRetrievalService {

    /**
     * Retrieves document chunks matching the query, evaluated under the default corporate authorization rules.
     * 
     * @param query the search query
     * @param username the username of the current user
     * @return the list of authorized document chunks
     */
    List<Document> retrieveAuthorizedChunks(String query, String username);

    /**
     * Retrieves document chunks matching the query, restricted to a specific department.
     * 
     * @param query the search query
     * @param username the username of the current user
     * @param department the department to restrict the search to
     * @return the list of authorized document chunks
     */
    List<Document> retrieveAuthorizedChunksWithDepartment(String query, String username, Department department);

    /**
     * Retrieves document chunks matching the query, restricted to documents owned by the user.
     * 
     * @param query the search query
     * @param username the username of the current user
     * @return the list of authorized document chunks
     */
    List<Document> retrieveAuthorizedChunksWithOwner(String query, String username);

    /**
     * Retrieves document chunks matching the query, restricted to specific document IDs.
     * 
     * @param query the search query
     * @param username the username of the current user
     * @param documentIds the list of allowed document IDs
     * @return the list of authorized document chunks
     */
    List<Document> retrieveAuthorizedChunksWithDocumentIds(String query, String username, List<Long> documentIds);
}
