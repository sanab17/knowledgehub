package com.enterprise.knowledgehub.controller;

import com.enterprise.knowledgehub.dto.DocumentResponseDto;
import com.enterprise.knowledgehub.dto.DocumentUploadDto;
import com.enterprise.knowledgehub.model.Department;
import com.enterprise.knowledgehub.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller for handling all document-related views and actions: listing, searching, uploading, downloading, and deleting.
 */
@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public String listDocuments(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "department", required = false) Department department,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "uploadDate") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String direction,
            Model model) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponseDto> documentsPage = documentService.searchDocuments(title, filename, department, pageable);

        model.addAttribute("documentsPage", documentsPage);
        model.addAttribute("title", title);
        model.addAttribute("filename", filename);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("departments", Department.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "documents/list";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("documentUploadDto", new DocumentUploadDto());
        model.addAttribute("departments", Department.values());
        return "documents/upload";
    }

    @PostMapping("/upload")
    public String uploadDocument(
            @ModelAttribute("documentUploadDto") @Valid DocumentUploadDto uploadDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", Department.values());
            return "documents/upload";
        }

        try {
            documentService.uploadDocument(uploadDto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMsg", "Document uploaded successfully!");
            return "redirect:/documents";
        } catch (Exception e) {
            log.error("Failed to upload document: {}", e.getMessage(), e);
            model.addAttribute("uploadError", e.getMessage());
            model.addAttribute("departments", Department.values());
            return "documents/upload";
        }
    }

    @GetMapping("/{id}")
    public String viewDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        DocumentResponseDto docDto = documentService.getDocumentById(id);
        model.addAttribute("document", docDto);
        model.addAttribute("currentUsername", userDetails.getUsername());
        model.addAttribute("isAdmin", userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        return "documents/view";
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        DocumentResponseDto docDto = documentService.getDocumentById(id);
        Resource fileResource = documentService.downloadDocument(id, userDetails.getUsername());

        String contentDisposition;
        try {
            // Encode filename to handle spaces and special characters gracefully
            String encodedFilename = URLEncoder.encode(docDto.getFilename(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            contentDisposition = "attachment; filename=\"" + docDto.getFilename() + "\"; filename*=UTF-8''" + encodedFilename;
        } catch (UnsupportedEncodingException e) {
            contentDisposition = "attachment; filename=\"" + docDto.getFilename() + "\"";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(docDto.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(fileResource);
    }

    @PostMapping("/delete/{id}")
    public String deleteDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.deleteDocument(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMsg", "Document deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting document ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete document: " + e.getMessage());
        }

        return "redirect:/documents";
    }
}
