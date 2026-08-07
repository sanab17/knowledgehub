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

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 20)
    private String result;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public String getBrowserName() {
        if (userAgent == null || userAgent.trim().isEmpty() || userAgent.equals("UNKNOWN")) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome") || ua.contains("crios")) return "Chrome";
        if (ua.contains("firefox") || ua.contains("fxios")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome") && !ua.contains("android")) return "Safari";
        if (ua.contains("edge") || ua.contains("edg")) return "Edge";
        if (ua.contains("msie") || ua.contains("trident")) return "Internet Explorer";
        if (ua.contains("python") || ua.contains("urllib")) return "Python / CLI";
        if (ua.contains("curl")) return "Curl / CLI";
        if (ua.contains("postman")) return "Postman / Tool";
        
        if (userAgent.length() > 20) {
            return userAgent.substring(0, 17) + "...";
        }
        return userAgent;
    }
}
