package com.enterprise.knowledgehub.dto;

import com.enterprise.knowledgehub.model.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Data Transfer Object containing document upload requests.
 */
@Data
public class DocumentUploadDto {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Department is required")
    private Department department;

    @NotNull(message = "Please select a file to upload")
    private MultipartFile file;
}
