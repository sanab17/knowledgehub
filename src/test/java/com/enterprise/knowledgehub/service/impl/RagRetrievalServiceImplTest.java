package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.model.Department;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.repository.UserRepository;
import com.enterprise.knowledgehub.security.RagAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RagRetrievalServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private UserRepository userRepository;

    @Spy
    private RagAuthorizationService ragAuthorizationService = new RagAuthorizationService();

    @InjectMocks
    private RagRetrievalServiceImpl ragRetrievalService;

    private User employeeUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        employeeUser = User.builder()
                .id(1L)
                .username("employee1")
                .role(UserRole.EMPLOYEE)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin1")
                .role(UserRole.ADMIN)
                .build();
    }

    @Test
    void testRetrieveAuthorizedChunks_AuthenticatedUserUnrestricted() {
        // Arrange
        String query = "company health insurance benefits";
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employeeUser));

        Document mockChunk = new Document("Health benefits overview text.", Map.of(
                "documentId", 10L,
                "title", "HR Guide",
                "filename", "hr-guide.pdf",
                "department", "HR",
                "owner", "employee1"
        ));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockChunk));

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunks(query, "employee1");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Health benefits overview text.", results.get(0).getText());

        // Verify SearchRequest was configured correctly
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertEquals(query, request.getQuery());
        assertEquals(4, request.getTopK());
        assertEquals(0.3, request.getSimilarityThreshold(), 0.001);
        // By default business rules, employees are unrestricted so filter expression is null or empty
        assertNull(request.getFilterExpression());
    }

    @Test
    void testRetrieveAuthorizedChunks_AdminIsAlwaysUnrestricted() {
        // Arrange
        String query = "confidential finance logs";
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunks(query, "admin1");

        // Assert
        assertTrue(results.isEmpty());
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertNull(request.getFilterExpression());
    }

    @Test
    void testRetrieveAuthorizedChunksWithDepartment_AppliesDepartmentFilter() {
        // Arrange
        String query = "engineering architecture guidelines";
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employeeUser));

        Document engChunk = new Document("Eng Architecture details...", Map.of(
                "documentId", 20L,
                "title", "Eng Arch",
                "filename", "arch.pdf",
                "department", "ENGINEERING",
                "owner", "admin"
        ));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(engChunk));

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunksWithDepartment(
                query, "employee1", Department.ENGINEERING
        );

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("ENGINEERING", results.get(0).getMetadata().get("department"));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        
        // Verifies the filter expression is correctly set for department-based restriction
        assertNotNull(request.getFilterExpression());
        String filterString = request.getFilterExpression().toString();
        assertTrue(filterString.contains("department") && filterString.contains("ENGINEERING"));
    }

    @Test
    void testRetrieveAuthorizedChunksWithOwner_AppliesOwnerFilter() {
        // Arrange
        String query = "my personal documents";
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employeeUser));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunksWithOwner(query, "employee1");

        // Assert
        assertTrue(results.isEmpty());
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertNotNull(request.getFilterExpression());
        String filterString = request.getFilterExpression().toString();
        assertTrue(filterString.contains("owner") && filterString.contains("employee1"));
    }

    @Test
    void testRetrieveAuthorizedChunksWithDocumentIds_AppliesDocumentListFilter() {
        // Arrange
        String query = "specific secure reports";
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employeeUser));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunksWithDocumentIds(
                query, "employee1", List.of(101L, 102L)
        );

        // Assert
        assertTrue(results.isEmpty());
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertNotNull(request.getFilterExpression());
        String filterString = request.getFilterExpression().toString();
        assertTrue(filterString.contains("documentId") && filterString.contains("101") && filterString.contains("102"));
    }

    @Test
    void testRetrieveAuthorizedChunks_UnauthenticatedOrNonexistentUser_DeniesAccess() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        List<Document> results = ragRetrievalService.retrieveAuthorizedChunks("query", "unknown");

        // Assert
        assertTrue(results.isEmpty());
        verifyNoInteractions(vectorStore);
    }
}
