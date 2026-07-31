package com.enterprise.knowledgehub.exception;

import com.enterprise.knowledgehub.exception.Exceptions.InvalidFileException;
import com.enterprise.knowledgehub.exception.Exceptions.RegistrationException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import com.enterprise.knowledgehub.exception.Exceptions.StorageException;
import com.enterprise.knowledgehub.exception.Exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice for handling all custom and general application exceptions.
 * Automatically intercepts errors and renders user-friendly Thymeleaf pages.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.error("Resource not found exception: {}", ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("errorTitle", "Document or Resource Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandlerFound(NoHandlerFoundException ex, Model model) {
        log.error("No handler found for path: {}", ex.getRequestURL());
        model.addAttribute("status", 404);
        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "The page you are looking for does not exist or has been moved.");
        return "error";
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized(UnauthorizedException ex, Model model) {
        log.error("Access denied: {}", ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler({StorageException.class, InvalidFileException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleFileExceptions(RuntimeException ex, Model model) {
        log.error("File storage or format error: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("errorTitle", "Invalid File Operation");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(RegistrationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRegistrationException(RegistrationException ex, Model model) {
        log.error("Registration error: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("errorTitle", "Registration Failed");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Internal server error occurred: ", ex);
        model.addAttribute("status", 500);
        model.addAttribute("errorTitle", "Internal Server Error");
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later or contact your system administrator.");
        return "error";
    }
}
