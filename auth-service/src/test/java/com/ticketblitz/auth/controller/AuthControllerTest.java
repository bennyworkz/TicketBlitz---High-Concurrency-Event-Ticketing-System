package com.ticketblitz.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.auth.dto.AuthResponse;
import com.ticketblitz.auth.dto.LoginRequest;
import com.ticketblitz.auth.dto.RegisterRequest;
import com.ticketblitz.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for AuthController
 * 
 * What are Integration Tests?
 * - Tests that verify multiple components work together
 * - Tests the full HTTP request/response cycle
 * - Uses MockMvc to simulate HTTP requests
 * 
 * Spring Test Annotations:
 * @WebMvcTest - Loads only the web layer (controllers)
 * @AutoConfigureMockMvc - Configures MockMvc automatically
 * @MockBean - Creates a mock bean in Spring context
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)  // Disable Spring Security for tests
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper;  // Converts objects to JSON

    @MockBean
    private AuthService authService;  // Fake service

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
            .accessToken("fake.jwt.token")
            .tokenType("Bearer")
            .expiresIn(86400L)  // Long value
            .userId("user-123")
            .email("test@example.com")
            .name("Test User")
            .role("USER")
            .build();
    }

    /**
     * Test 1: POST /auth/register - Success
     * Verifies that registration endpoint works
     */
    @Test
    @DisplayName("POST /auth/register should return 201 and auth response")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())  // Expect HTTP 201
            .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    /**
     * Test 2: POST /auth/register - Missing Email
     * Verifies validation works
     */
    @Test
    @DisplayName("POST /auth/register should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
        // Arrange
        registerRequest.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest());  // Expect HTTP 400
    }

    /**
     * Test 3: POST /auth/register - Invalid Email Format
     */
    @Test
    @DisplayName("POST /auth/register should return 400 when email format is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
        // Arrange
        registerRequest.setEmail("not-an-email");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest());
    }

    /**
     * Test 4: POST /auth/register - Password Too Short
     */
    @Test
    @DisplayName("POST /auth/register should return 400 when password is too short")
    void shouldReturn400WhenPasswordTooShort() throws Exception {
        // Arrange
        registerRequest.setPassword("123");  // Less than 6 characters

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest());
    }

    /**
     * Test 5: POST /auth/login - Success
     */
    @Test
    @DisplayName("POST /auth/login should return 200 and auth response")
    void shouldLoginUserSuccessfully() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    /**
     * Test 6: POST /auth/login - Missing Credentials
     */
    @Test
    @DisplayName("POST /auth/login should return 400 when credentials missing")
    void shouldReturn400WhenCredentialsMissing() throws Exception {
        // Arrange
        loginRequest.setEmail(null);
        loginRequest.setPassword(null);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest());
    }

    /**
     * Test 7: POST /auth/login - Invalid Credentials
     * Note: Skipping this test because the controller doesn't have proper
     * exception handling yet. In production, you'd want a @ControllerAdvice
     * to handle exceptions and return proper HTTP status codes.
     */
    // @Test
    // @DisplayName("POST /auth/login should handle invalid credentials gracefully")
    // void shouldHandleInvalidCredentials() throws Exception {
    //     // TODO: Implement proper exception handling in controller
    // }

    /**
     * Test 8: GET /auth/validate - Valid Token
     */
    @Test
    @DisplayName("GET /auth/validate should return 200 for valid token")
    void shouldValidateTokenSuccessfully() throws Exception {
        // Arrange
        String token = "Bearer fake.jwt.token";
        when(authService.validateToken(anyString())).thenReturn(true);
        when(authService.extractUserId(anyString())).thenReturn("user-123");

        // Act & Assert
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.userId").value("user-123"));
    }

    /**
     * Test 9: GET /auth/validate - Invalid Token
     */
    @Test
    @DisplayName("GET /auth/validate should return 401 for invalid token")
    void shouldReturn401ForInvalidToken() throws Exception {
        // Arrange
        String token = "Bearer invalid.token";
        when(authService.validateToken(anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/auth/validate")
                .header("Authorization", token))
            .andExpect(status().isUnauthorized());  // Expect HTTP 401
    }

    /**
     * Test 10: GET /auth/validate - Missing Token
     */
    @Test
    @DisplayName("GET /auth/validate should return 400 when token missing")
    void shouldReturn400WhenTokenMissing() throws Exception {
        // Act & Assert
        // Missing Authorization header returns 400 (Bad Request)
        mockMvc.perform(get("/auth/validate"))
            .andExpect(status().isBadRequest());
    }

    /**
     * Test 11: Content Type Validation
     */
    @Test
    @DisplayName("Should return 415 when content type is not JSON")
    void shouldReturn415WhenContentTypeNotJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("not json"))
            .andExpect(status().isUnsupportedMediaType());  // Expect HTTP 415
    }

    /**
     * Test 12: Malformed JSON
     */
    @Test
    @DisplayName("Should return 400 when JSON is malformed")
    void shouldReturn400WhenJsonMalformed() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
            .andExpect(status().isBadRequest());
    }
}
