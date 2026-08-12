package com.enterprise.knowledgehub.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new document is successfully uploaded and stored.
 * This event triggers the asynchronous text parsing, chunking, and embedding generation pipeline (RAG).
 */
@Getter
public class DocumentUploadedEvent extends ApplicationEvent {
    private final Long documentId;
    private final String filename;

    public DocumentUploadedEvent(Object source, Long documentId, String filename) {
        super(source);
        this.documentId = documentId;
        this.filename = filename;
    }
}
