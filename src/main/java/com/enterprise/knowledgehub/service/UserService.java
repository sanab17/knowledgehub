package com.enterprise.knowledgehub.service;

import com.enterprise.knowledgehub.dto.UserRegistrationDto;
import com.enterprise.knowledgehub.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for user operations and authentication.
 * Extends UserDetailsService for integration with Spring Security.
 */
public interface UserService extends UserDetailsService {

    /**
     * Registers a new user with BCrypt hashed password and validated input.
     * @param registrationDto registration details
     * @return the registered User entity
     */
    User registerUser(UserRegistrationDto registrationDto);

    /**
     * Fetches a User by username.
     */
    User getUserByUsername(String username);

    /**
     * Checks if a username exists in the system.
     */
    boolean existsByUsername(String username);
}
