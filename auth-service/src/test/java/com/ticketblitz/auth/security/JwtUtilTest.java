package com.ticketblitz.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for JwtUtil
 * 
 * What are Unit Tests?
 * - Tests that verify a single "unit" of code (like one class or method)
 * - Run fast (no database, no network)
 * - Help catch bugs early
 * 
 * JUnit Annotations:
 * @Test - Marks a method as a test
 * @BeforeEach - Runs before each test (setup)
 * @DisplayName - Human-readable test name
 */
@DisplayName("JWT Utility Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String TEST_SECRET = "TestSecretKeyForJWTTokenGenerationMustBeLongEnough256BitsForTesting";
    private final long TEST_EXPIRATION = 86400000L; // 24 hours

    /**
     * Setup method - runs before each test
     * Creates a fresh JwtUtil instance for each test
     */
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set private fields using reflection (for testing only)
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    /**
     * Test 1: Generate Token
     * Verifies that we can create a JWT token
     */
    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // Arrange (Setup test data)
        String userId = "user-123";
        String email = "test@example.com";
        String role = "USER";

        // Act (Execute the method we're testing)
        String token = jwtUtil.generateToken(userId, email, role);

        // Assert (Verify the results)
        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");
        assertTrue(token.startsWith("eyJ"), "JWT should start with 'eyJ'");
        
        // JWT has 3 parts separated by dots: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
    }

    /**
     * Test 2: Extract User ID from Token
     * Verifies we can get the user ID back from a token
     */
    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Arrange
        String userId = "user-456";
        String email = "test@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(userId, email, role);

        // Act
        String extractedUserId = jwtUtil.extractUserId(token);

        // Assert
        assertEquals(userId, extractedUserId, "Extracted user ID should match original");
    }

    /**
     * Test 3: Extract Email from Token
     */
    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Arrange
        String userId = "user-789";
        String email = "john@example.com";
        String role = "ADMIN";
        String token = jwtUtil.generateToken(userId, email, role);

        // Act
        String extractedEmail = jwtUtil.extractEmail(token);

        // Assert
        assertEquals(email, extractedEmail, "Extracted email should match original");
    }

    /**
     * Test 4: Extract Role from Token
     */
    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() {
        // Arrange
        String userId = "user-999";
        String email = "admin@example.com";
        String role = "ADMIN";
        String token = jwtUtil.generateToken(userId, email, role);

        // Act
        String extractedRole = jwtUtil.extractRole(token);

        // Assert
        assertEquals(role, extractedRole, "Extracted role should match original");
    }

    /**
     * Test 5: Validate Valid Token
     * Verifies that a valid token passes validation
     */
    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        // Arrange
        String userId = "user-111";
        String email = "valid@example.com";
        String role = "USER";
        String token = jwtUtil.generateToken(userId, email, role);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid, "Valid token should pass validation");
    }

    /**
     * Test 6: Reject Invalid Token
     * Verifies that an invalid token fails validation
     */
    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid, "Invalid token should fail validation");
    }

    /**
     * Test 7: Reject Null Token
     */
    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // Act
        boolean isValid = jwtUtil.validateToken(null);

        // Assert
        assertFalse(isValid, "Null token should fail validation");
    }

    /**
     * Test 8: Reject Empty Token
     */
    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // Act
        boolean isValid = jwtUtil.validateToken("");

        // Assert
        assertFalse(isValid, "Empty token should fail validation");
    }

    /**
     * Test 9: Token Contains Correct Claims
     * Verifies all expected data is in the token
     */
    @Test
    @DisplayName("Should include all claims in token")
    void shouldIncludeAllClaimsInToken() {
        // Arrange
        String userId = "user-222";
        String email = "claims@example.com";
        String role = "USER";

        // Act
        String token = jwtUtil.generateToken(userId, email, role);

        // Assert - Extract all claims and verify
        assertEquals(userId, jwtUtil.extractUserId(token));
        assertEquals(email, jwtUtil.extractEmail(token));
        assertEquals(role, jwtUtil.extractRole(token));
    }

    /**
     * Test 10: Different Users Get Different Tokens
     */
    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Arrange
        String token1 = jwtUtil.generateToken("user-1", "user1@example.com", "USER");
        String token2 = jwtUtil.generateToken("user-2", "user2@example.com", "USER");

        // Assert
        assertNotEquals(token1, token2, "Different users should have different tokens");
    }
}
