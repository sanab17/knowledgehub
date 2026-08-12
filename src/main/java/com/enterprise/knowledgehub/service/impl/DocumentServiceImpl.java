package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.dto.DocumentResponseDto;
import com.enterprise.knowledgehub.dto.DocumentUploadDto;
import com.enterprise.knowledgehub.exception.Exceptions.InvalidFileException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import com.enterprise.knowledgehub.exception.Exceptions.UnauthorizedException;
import com.enterprise.knowledgehub.model.Department;
import com.enterprise.knowledgehub.model.Document;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.repository.DocumentRepository;
import com.enterprise.knowledgehub.repository.UserRepository;
import com.enterprise.knowledgehub.service.DocumentService;
import com.enterprise.knowledgehub.service.StorageService;
import com.enterprise.knowledgehub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import com.enterprise.knowledgehub.event.DocumentUploadedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for managing document lifecycle and enforcing security
 * rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.upload.allowed-types}")
    private List<String> allowedTypes;

    @Value("${app.upload.max-size-bytes}")
    private long maxSizeBytes;

    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(DocumentUploadDto uploadDto, String username) {
        log.info("User '{}' initiating upload for document title: '{}'", username, uploadDto.getTitle());

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        MultipartFile file = uploadDto.getFile();
        try {
            validateFile(file);
        } catch (InvalidFileException e) {
            auditLogService.log("UPLOAD", null, uploadDto.getTitle().trim(), username,
                    "Upload failed: " + e.getMessage(), "FAILURE");
            throw e;
        }

        // Store file physically
        String savedFilename = storageService.store(file);

        // Build metadata entity
        Document document = Document.builder()
                .title(uploadDto.getTitle().trim())
                .description(uploadDto.getDescription() != null ? uploadDto.getDescription().trim() : null)
                .filename(savedFilename)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .department(uploadDto.getDepartment())
                .owner(owner)
                .build();

        Document savedDoc = documentRepository.save(document);
        log.info("Document successfully uploaded. ID: {}, Title: '{}', Saved Filename: '{}', Owner: '{}'",
                savedDoc.getId(), savedDoc.getTitle(), savedDoc.getFilename(), username);

        auditLogService.log("UPLOAD", savedDoc.getId(), savedDoc.getTitle(), username,
                "Document uploaded successfully");

        // Trigger asynchronous RAG text ingestion & vectorization
        eventPublisher.publishEvent(new DocumentUploadedEvent(this, savedDoc.getId(), savedDoc.getFilename()));

        return mapToResponseDto(savedDoc);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file cannot be empty");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            // Also validate by extension in case browser content-type is generic
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
                log.error("File upload rejected. Invalid content type or extension: {}", contentType);
                throw new InvalidFileException("Invalid file type. Only PDF and DOCX files are allowed.");
            }
        }

        // Validate size
        if (file.getSize() > maxSizeBytes) {
            log.error("File upload rejected. File size {} exceeds limit of {} bytes", file.getSize(), maxSizeBytes);
            throw new InvalidFileException(
                    "File exceeds maximum allowed size of " + (maxSizeBytes / (1024 * 1024)) + "MB");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> searchDocuments(String title, String filename, Department department,
            Pageable pageable) {
        Specification<Document> spec = Specification.where(null);

        if (title != null && !title.trim().isEmpty()) {
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
        }
        if (filename != null && !filename.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("filename")),
                    "%" + filename.trim().toLowerCase() + "%"));
        }
        if (department != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department"), department));
        }

        return documentRepository.findAll(spec, pageable).map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDto getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));
        return mapToResponseDto(document);
    }

    @Override
    @Transactional
    public Resource downloadDocument(Long id, String username) {
        log.info("User '{}' requesting download for document ID: {}", username, id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        Resource resource = storageService.loadAsResource(document.getFilename());
        log.info("Document downloaded successfully. ID: {}, Title: '{}', User: '{}'", id, document.getTitle(),
                username);

        auditLogService.log("DOWNLOAD", id, document.getTitle(), username, "Document downloaded successfully");

        return resource;
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, String username) {
        log.info("User '{}' requesting deletion of document ID: {}", username, id);

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));

        // Security check: Only document owner or an ADMIN can delete
        boolean isOwner = document.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Access Denied: User '{}' is not authorized to delete document ID: {}", username, id);
            auditLogService.log("DELETE", id, document.getTitle(), username, "Unauthorized delete attempt blocked",
                    "FAILURE");
            throw new UnauthorizedException("You are not authorized to delete this document.");
        }

        // Delete physical file
        storageService.delete(document.getFilename());

        // Delete vector embeddings from vector store
        try {
            int deletedChunks = jdbcTemplate
                    .update("DELETE FROM vector_store WHERE (metadata->>'documentId')::bigint = ?", id);
            log.info("Pruned {} vector chunks from vector_store for document ID: {}", deletedChunks, id);
        } catch (Exception e) {
            log.warn("Could not delete vector chunks for document ID: {}. Error: {}", id, e.getMessage());
        }

        // Delete database record
        documentRepository.delete(document);

        auditLogService.log("DELETE", id, document.getTitle(), username, "Document deleted by " + username);

        log.info("Document deleted successfully. ID: {}, Title: '{}', Requestor: '{}'", id, document.getTitle(),
                username);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalDocumentCount() {
        return documentRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyDocumentCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return documentRepository.countByOwner(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getRecentDocuments() {
        return documentRepository.findTop5ByOrderByUploadDateDesc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDepartmentStats() {
        Map<String, Long> stats = new HashMap<>();
        // Initialize all departments with zero
        for (Department dept : Department.values()) {
            stats.put(dept.name(), 0L);
        }

        List<Object[]> rawStats = documentRepository.countDocumentsByDepartment();
        for (Object[] row : rawStats) {
            Department dept = (Department) row[0];
            Long count = (Long) row[1];
            if (dept != null) {
                stats.put(dept.name(), count);
            }
        }
        return stats;
    }

    private DocumentResponseDto mapToResponseDto(Document doc) {
        return DocumentResponseDto.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .filename(doc.getFilename())
                .fileSize(doc.getFileSize())
                .contentType(doc.getContentType())
                .department(doc.getDepartment())
                .ownerUsername(doc.getOwner().getUsername())
                .ownerId(doc.getOwner().getId())
                .uploadDate(doc.getUploadDate())
                .build();
    }
}
