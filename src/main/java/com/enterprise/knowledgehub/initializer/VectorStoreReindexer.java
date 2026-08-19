package com.enterprise.knowledgehub.initializer;

import com.enterprise.knowledgehub.event.DocumentUploadedEvent;
import com.enterprise.knowledgehub.model.Document;
import com.enterprise.knowledgehub.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup component that scans existing documents and ensures they are vectorized in the pgvector store.
 * If a document has 0 chunks, it triggers the asynchronous ingestion process.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreReindexer implements CommandLineRunner {

    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void run(String... args) {
        log.info("RAG Reindexer: Checking for documents missing from vector store...");
        List<Document> documents = documentRepository.findAll();

        for (Document doc : documents) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vector_store WHERE (metadata->>'documentId')::bigint = ?",
                        Integer.class,
                        doc.getId()
                );

                if (count == null || count == 0) {
                    log.info("RAG Reindexer: Document '{}' (ID: {}) is not vectorized. Triggering ingestion...",
                            doc.getTitle(), doc.getId());
                    eventPublisher.publishEvent(new DocumentUploadedEvent(this, doc.getId(), doc.getFilename()));
                } else {
                    log.debug("RAG Reindexer: Document '{}' (ID: {}) already has {} chunks in vector store.",
                            doc.getTitle(), doc.getId(), count);
                }
            } catch (Exception e) {
                log.error("RAG Reindexer: Failed to check/trigger indexing for document ID: {}. Error: {}",
                        doc.getId(), e.getMessage(), e);
            }
        }
    }
}
