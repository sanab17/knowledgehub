package com.enterprise.knowledgehub.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller mapping for global error handling.
 * Resolves container-level errors (403, 404, 500) and supplies descriptive models for Thymeleaf rendering.
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String statusParam = request.getParameter("status");
        
        int statusCode = 500;
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {}
        } else if (statusParam != null) {
            try {
                statusCode = Integer.parseInt(statusParam);
            } catch (NumberFormatException ignored) {}
        }
        
        log.error("Global error handler triggered. Status code: {}", statusCode);
        model.addAttribute("status", statusCode);
        
        if (statusCode == 404) {
            model.addAttribute("errorTitle", "Page Not Found");
            model.addAttribute("errorMessage", "The page you are looking for does not exist, has been moved, or you typed the wrong URL.");
        } else if (statusCode == 403) {
            model.addAttribute("errorTitle", "Access Denied");
            model.addAttribute("errorMessage", "You do not have the required permissions or administrator credentials to view this page.");
        } else if (statusCode == 400) {
            model.addAttribute("errorTitle", "Bad Request");
            model.addAttribute("errorMessage", "The server could not understand or execute the request due to malformed syntax.");
        } else {
            model.addAttribute("errorTitle", "Unexpected Error");
            model.addAttribute("errorMessage", "An unexpected error occurred on our servers. Please try again later or contact your system administrator.");
        }
        
        return "error";
    }
}
