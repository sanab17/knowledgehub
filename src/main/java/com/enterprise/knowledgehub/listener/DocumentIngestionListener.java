package com.enterprise.knowledgehub.listener;

import com.enterprise.knowledgehub.event.DocumentUploadedEvent;
import com.enterprise.knowledgehub.model.Document;
import com.enterprise.knowledgehub.repository.DocumentRepository;
import com.enterprise.knowledgehub.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Listener that catches document upload events and processes them asynchronously.
 * Extracts raw text (PDF or DOCX), splits it into chunks, and vectorizes it via Spring AI pgvector.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionListener {

    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    @Async
    @EventListener
    @Transactional
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        log.info("RAG: Received document uploaded event for document ID: {}", event.getDocumentId());
        
        Optional<Document> documentOpt = documentRepository.findById(event.getDocumentId());
        if (documentOpt.isEmpty()) {
            log.error("RAG: Document ID {} not found in database. Skipping ingestion.", event.getDocumentId());
            return;
        }
        Document doc = documentOpt.get();

        try {
            // 1. Load document resource from storage service
            Resource fileResource = storageService.loadAsResource(doc.getFilename());
            
            // 2. Parse text contents
            String rawText;
            try (InputStream is = fileResource.getInputStream()) {
                rawText = extractText(is, doc.getFilename());
            }

            if (rawText == null || rawText.trim().isEmpty()) {
                log.warn("RAG: Extracted text is empty for document ID: {}. Skipping vectorization.", doc.getId());
                return;
            }

            log.info("RAG: Successfully parsed text for document '{}'. Length: {} chars.", doc.getTitle(), rawText.length());

            // 3. Split text into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            
            // Configure metadata for similarity searches and pruning
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", doc.getId());
            metadata.put("title", doc.getTitle());
            metadata.put("filename", doc.getFilename());
            metadata.put("department", doc.getDepartment().name());
            metadata.put("owner", doc.getOwner().getUsername());

            List<org.springframework.ai.document.Document> chunks = splitter.split(
                new org.springframework.ai.document.Document(rawText, metadata)
            );

            log.info("RAG: Split document '{}' into {} chunks. Storing in pgvector...", doc.getTitle(), chunks.size());

            // 4. Save to Vector Store (embeddings are generated automatically via active EmbeddingClient)
            vectorStore.accept(chunks);
            log.info("RAG: Successfully stored vectors for document ID: {} in database.", doc.getId());

        } catch (Exception e) {
            log.error("RAG: Failed to ingest document ID: {}. Error: {}", doc.getId(), e.getMessage(), e);
        }
    }

    private String extractText(InputStream is, String filename) throws IOException {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return extractPdfText(is);
        } else if (lowerName.endsWith(".docx")) {
            return extractDocxText(is);
        } else {
            throw new IllegalArgumentException("Unsupported file type for parser: " + filename);
        }
    }

    private String extractPdfText(InputStream is) throws IOException {
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }
    }
}
