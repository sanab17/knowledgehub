package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.model.Department;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.repository.UserRepository;
import com.enterprise.knowledgehub.security.RagAuthorizationService;
import com.enterprise.knowledgehub.service.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of RagRetrievalService incorporating document-level metadata filtering.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final VectorStore vectorStore;
    private final UserRepository userRepository;
    private final RagAuthorizationService ragAuthorizationService;

    @Override
    public List<Document> retrieveAuthorizedChunks(String query, String username) {
        log.info("RAG: Retrieving authorized chunks for user '{}' and query '{}'", username, query);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("RAG: User '{}' not found in database. Returning empty contexts.", username);
            return Collections.emptyList();
        }
        User user = userOpt.get();
        String filterExpression = ragAuthorizationService.getFilterExpressionForUser(user);
        return executeSearch(query, filterExpression);
    }

    @Override
    public List<Document> retrieveAuthorizedChunksWithDepartment(String query, String username, Department department) {
        log.info("RAG: Retrieving department-restricted chunks for user '{}', department '{}' and query '{}'", username, department, query);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("RAG: User '{}' not found in database. Returning empty contexts.", username);
            return Collections.emptyList();
        }
        User user = userOpt.get();
        String filterExpression = ragAuthorizationService.getFilterExpressionWithDepartment(user, department);
        return executeSearch(query, filterExpression);
    }

    @Override
    public List<Document> retrieveAuthorizedChunksWithOwner(String query, String username) {
        log.info("RAG: Retrieving owner-restricted chunks for user '{}' and query '{}'", username, query);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("RAG: User '{}' not found in database. Returning empty contexts.", username);
            return Collections.emptyList();
        }
        User user = userOpt.get();
        String filterExpression = ragAuthorizationService.getFilterExpressionWithOwner(user);
        return executeSearch(query, filterExpression);
    }

    @Override
    public List<Document> retrieveAuthorizedChunksWithDocumentIds(String query, String username, List<Long> documentIds) {
        log.info("RAG: Retrieving specific documents-restricted chunks for user '{}' and query '{}'", username, query);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("RAG: User '{}' not found in database. Returning empty contexts.", username);
            return Collections.emptyList();
        }
        User user = userOpt.get();
        String filterExpression = ragAuthorizationService.getFilterExpressionWithDocuments(user, documentIds);
        return executeSearch(query, filterExpression);
    }

    private List<Document> executeSearch(String query, String filterExpression) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(0.3);

        if (filterExpression != null && !filterExpression.isEmpty()) {
            log.debug("RAG: Applying filter expression: '{}'", filterExpression);
            builder.filterExpression(filterExpression);
        }

        try {
            return vectorStore.similaritySearch(builder.build());
        } catch (Exception e) {
            log.error("RAG: Error performing similarity search. Error: {}", e.getMessage(), e);
            throw e;
        }
    }
}
