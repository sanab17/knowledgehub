package com.enterprise.knowledgehub.controller;

import com.enterprise.knowledgehub.model.AuditLog;
import com.enterprise.knowledgehub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller restricted to Administrator operations, such as viewing portal audit trail logs.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit-logs")
    public String listAuditLogs(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sortBy", defaultValue = "timestamp") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String direction,
            Model model) {

        log.info("Admin requesting audit logs page {} with size {}. Filters -> username: {}", page, size, username);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLog> logsPage = auditLogService.getAllLogs(username, pageable);

        model.addAttribute("logsPage", logsPage);
        model.addAttribute("searchUsername", username);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/audit-logs";
    }
}
