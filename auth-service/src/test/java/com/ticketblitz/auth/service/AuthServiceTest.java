package com.ticketblitz.auth.service;

import com.ticketblitz.auth.dto.AuthResponse;
import com.ticketblitz.auth.dto.LoginRequest;
import com.ticketblitz.auth.dto.RegisterRequest;
import com.ticketblitz.auth.entity.User;
import com.ticketblitz.auth.repository.UserRepository;
import com.ticketblitz.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService
 * 
 * What is Mocking?
 * - Mocking creates "fake" versions of dependencies
 * - We don't need a real database or JWT generator
 * - Tests run faster and are more reliable
 * 
 * Mockito Annotations:
 * @Mock - Creates a fake object
 * @InjectMocks - Creates the object we're testing and injects mocks into it
 * @ExtendWith(MockitoExtension.class) - Enables Mockito in JUnit 5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;  // Fake database

    @Mock
    private PasswordEncoder passwordEncoder;  // Fake password encoder

    @Mock
    private JwtUtil jwtUtil;  // Fake JWT generator

    @InjectMocks
    private AuthService authService;  // Real service with fake dependencies

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    /**
     * Setup - runs before each test
     * Creates test data that all tests can use
     */
    @BeforeEach
    void setUp() {
        // Create a registration request
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setName("Test User");

        // Create a login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Create a user entity
        user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setPassword("$2a$10$hashedPassword");  // BCrypt hash
        user.setName("Test User");
        user.setRole("USER");
    }

    /**
     * Test 1: Successful Registration
     * Verifies that a new user can register
     */
    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Arrange
        // Tell the mock: "When someone checks if email exists, say NO"
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        
        // Tell the mock: "When someone encodes a password, return this hash"
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        
        // Tell the mock: "When someone saves a user, return this user"
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Tell the mock: "When someone generates a token, return this token"
        when(jwtUtil.generateToken(anyString(), anyString(), anyString()))
            .thenReturn("fake.jwt.token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals("fake.jwt.token", response.getAccessToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals("USER", response.getRole());
        assertEquals("Bearer", response.getTokenType());
        // Note: expiresIn comes from jwtUtil.getExpirationInSeconds()
        // which we need to mock properly

        // Verify that methods were called
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(anyString(), anyString(), anyString());
    }

    /**
     * Test 2: Registration with Existing Email
     * Verifies that duplicate emails are rejected
     */
    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        // assertThrows checks that an exception is thrown
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email already registered", exception.getMessage());
        
        // Verify that save was never called (because email exists)
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test 3: Successful Login
     * Verifies that a user can login with correct credentials
     */
    @Test
    @DisplayName("Should login user successfully with correct credentials")
    void shouldLoginSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString()))
            .thenReturn("fake.jwt.token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("fake.jwt.token", response.getAccessToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("user-123", response.getUserId());

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "$2a$10$hashedPassword");
    }

    /**
     * Test 4: Login with Wrong Email
     * Verifies that login fails with non-existent email
     */
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    /**
     * Test 5: Login with Wrong Password
     * Verifies that login fails with incorrect password
     */
    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void shouldThrowExceptionWhenPasswordIncorrect() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    /**
     * Test 6: Validate Valid Token
     */
    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        // Arrange
        String token = "valid.jwt.token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractUserId(token)).thenReturn("user-123");

        // Act
        boolean isValid = authService.validateToken(token);
        String userId = jwtUtil.extractUserId(token);

        // Assert
        assertTrue(isValid);
        assertEquals("user-123", userId);
    }

    /**
     * Test 7: Validate Invalid Token
     */
    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        // Arrange
        String token = "invalid.jwt.token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    /**
     * Test 8: Password is Hashed Before Saving
     * Verifies that we never save plain text passwords
     */
    @Test
    @DisplayName("Should hash password before saving")
    void shouldHashPasswordBeforeSaving() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString()))
            .thenReturn("fake.jwt.token");

        // Act
        authService.register(registerRequest);

        // Assert
        // Verify that encode was called with the plain password
        verify(passwordEncoder, times(1)).encode("password123");
        
        // Verify that save was called (password should be hashed inside)
        verify(userRepository, times(1)).save(any(User.class));
    }
}
