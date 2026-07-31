package com.enterprise.knowledgehub.controller;

import com.enterprise.knowledgehub.dto.UserRegistrationDto;
import com.enterprise.knowledgehub.exception.Exceptions.RegistrationException;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller for user authentication and self-registration.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // If already logged in, skip the login screen
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setRole(UserRole.EMPLOYEE); // Default selection
        model.addAttribute("user", dto);
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(
            @ModelAttribute("user") @Valid UserRegistrationDto registrationDto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(registrationDto);
            log.info("Self-registration successful for user: {}", registrationDto.getUsername());
            return "redirect:/login?registered=true";
        } catch (RegistrationException e) {
            log.error("Registration error for user {}: {}", registrationDto.getUsername(), e.getMessage());
            model.addAttribute("registrationError", e.getMessage());
            return "register";
        }
    }
}
