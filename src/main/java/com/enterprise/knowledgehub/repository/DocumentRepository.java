package com.enterprise.knowledgehub.repository;

import com.enterprise.knowledgehub.model.Document;
import com.enterprise.knowledgehub.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing Document entity database operations.
 * Extends JpaSpecificationExecutor to allow dynamic, type-safe filtering and searching.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    /**
     * Counts the total number of documents uploaded by a specific user.
     */
    long countByOwner(User owner);

    /**
     * Fetches the recently uploaded documents (e.g., top 5).
     */
    List<Document> findTop5ByOrderByUploadDateDesc();

    /**
     * Computes the document count aggregated by department.
     * Useful for dashboard stats and chart visualization.
     */
    @Query("SELECT d.department, COUNT(d) FROM Document d GROUP BY d.department")
    List<Object[]> countDocumentsByDepartment();
}
