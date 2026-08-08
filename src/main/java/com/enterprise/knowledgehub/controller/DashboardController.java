package com.enterprise.knowledgehub.controller;

import com.enterprise.knowledgehub.dto.DocumentResponseDto;
import com.enterprise.knowledgehub.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * Controller for rendering the application homepage/dashboard.
 * Collects and serves summary metrics, recent documents, and department-level stats.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DocumentService documentService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        log.info("Loading dashboard metrics for user: {}", username);

        long totalDocs = documentService.getTotalDocumentCount();
        long myDocs = documentService.getMyDocumentCount(username);
        List<DocumentResponseDto> recentDocs = documentService.getRecentDocuments();
        Map<String, Long> deptStats = documentService.getDepartmentStats();

        model.addAttribute("totalDocuments", totalDocs);
        model.addAttribute("myDocuments", myDocs);
        model.addAttribute("recentDocuments", recentDocs);
        model.addAttribute("departmentStats", deptStats);
        model.addAttribute("username", username);

        // Add a list of departments for the sidebar or reference
        return "dashboard";
    }
}
