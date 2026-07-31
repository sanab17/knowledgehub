package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.dto.UserRegistrationDto;
import com.enterprise.knowledgehub.exception.Exceptions.RegistrationException;
import com.enterprise.knowledgehub.exception.Exceptions.ResourceNotFoundException;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.repository.UserRepository;
import com.enterprise.knowledgehub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Service implementation for managing users and integrating with Spring Security authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        log.info("Attempting to register user: {}", dto.getUsername());

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RegistrationException("Passwords do not match");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RegistrationException("Username is already taken");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RegistrationException("Email is already registered");
        }

        UserRole role = dto.getRole() != null ? dto.getRole() : UserRole.EMPLOYEE;

        User user = User.builder()
                .username(dto.getUsername().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} with role {}", savedUser.getUsername(), savedUser.getRole());
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login attempt failed. Username not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.info("User login successful: {}", username);

        // Prefix authority with ROLE_ as expected by Spring Security hasRole
        String roleName = "ROLE_" + user.getRole().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        // Return core UserDetails implementation
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
