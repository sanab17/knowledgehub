package com.enterprise.knowledgehub.dto;

import com.enterprise.knowledgehub.model.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for responding with document metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {
    private Long id;
    private String title;
    private String description;
    private String filename;
    private Long fileSize;
    private String contentType;
    private Department department;
    private String ownerUsername;
    private Long ownerId;
    private LocalDateTime uploadDate;

    /**
     * Helper method to return human-readable file sizes.
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 Bytes";
        if (fileSize < 1024) return fileSize + " Bytes";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }
}
