package com.enterprise.knowledgehub.security;

import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.model.Department;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to generate Spring AI vector store metadata filters based on user roles and permissions.
 */
@Service
@Slf4j
public class RagAuthorizationService {

    /**
     * Builds the authorization filter expression for Spring AI vector store searches.
     * 
     * @param user the current authenticated user
     * @return a Spring AI filter expression string, or empty string/null if unrestricted.
     */
    public String getFilterExpressionForUser(User user) {
        if (user == null) {
            log.warn("No authenticated user context provided for RAG retrieval. Denying access.");
            return "documentId == -1"; // Matches nothing
        }

        if (user.getRole() == UserRole.ADMIN) {
            log.info("User '{}' is ADMIN. Vector search is unrestricted.", user.getUsername());
            return "";
        }

        // Under current business rules, authenticated employees are allowed to read all documents.
        // Therefore, we return an empty filter expression to allow complete access.
        log.info("User '{}' is EMPLOYEE. Vector search is unrestricted by default business rules.", user.getUsername());
        return "";
    }

    /**
     * Builds a department-restricted filter expression.
     * 
     * @param user the current authenticated user
     * @param department the department to restrict search to
     * @return a Spring AI filter expression string
     */
    public String getFilterExpressionWithDepartment(User user, Department department) {
        if (user == null) {
            return "documentId == -1";
        }
        if (user.getRole() == UserRole.ADMIN) {
            return "";
        }
        return "department == '" + department.name() + "'";
    }

    /**
     * Builds an owner-restricted filter expression.
     * 
     * @param user the current authenticated user
     * @return a Spring AI filter expression string
     */
    public String getFilterExpressionWithOwner(User user) {
        if (user == null) {
            return "documentId == -1";
        }
        if (user.getRole() == UserRole.ADMIN) {
            return "";
        }
        return "owner == '" + user.getUsername() + "'";
    }

    /**
     * Builds a specific document IDs restricted filter expression.
     * 
     * @param user the current authenticated user
     * @param documentIds list of document IDs the user is allowed to access
     * @return a Spring AI filter expression string
     */
    public String getFilterExpressionWithDocuments(User user, List<Long> documentIds) {
        if (user == null || documentIds == null || documentIds.isEmpty()) {
            return "documentId == -1";
        }
        if (user.getRole() == UserRole.ADMIN) {
            return "";
        }
        String listStr = documentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return "documentId in [" + listStr + "]";
    }
}
