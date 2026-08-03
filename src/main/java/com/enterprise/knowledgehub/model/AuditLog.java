package com.enterprise.knowledgehub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry for security and data compliance tracking.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action; // e.g. UPLOAD, DOWNLOAD, DELETE

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_title", nullable = false, length = 100)
    private String documentTitle;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 255)
    private String details;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
