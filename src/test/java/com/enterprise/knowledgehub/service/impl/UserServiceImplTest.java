package com.enterprise.knowledgehub.service.impl;

import com.enterprise.knowledgehub.dto.UserRegistrationDto;
import com.enterprise.knowledgehub.exception.Exceptions.RegistrationException;
import com.enterprise.knowledgehub.model.User;
import com.enterprise.knowledgehub.model.UserRole;
import com.enterprise.knowledgehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .email("testuser@company.com")
                .password("password123")
                .confirmPassword("password123")
                .role(UserRole.EMPLOYEE)
                .build();
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@company.com")
                .password("hashedPassword")
                .role(UserRole.EMPLOYEE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.registerUser(registrationDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_PasswordsDoNotMatch_ThrowsException() {
        // Arrange
        registrationDto.setConfirmPassword("differentPassword");

        // Act & Assert
        RegistrationException exception = assertThrows(RegistrationException.class, () -> {
            userService.registerUser(registrationDto);
        });
        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_UsernameTaken_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // Act & Assert
        RegistrationException exception = assertThrows(RegistrationException.class, () -> {
            userService.registerUser(registrationDto);
        });
        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
