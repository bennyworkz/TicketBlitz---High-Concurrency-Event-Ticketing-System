package com.ticketblitz.auth.controller;

import com.ticketblitz.auth.dto.AuthResponse;
import com.ticketblitz.auth.dto.LoginRequest;
import com.ticketblitz.auth.dto.RegisterRequest;
import com.ticketblitz.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and JWT token management APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * Register new user
     * POST /auth/register
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with email and password. Returns a JWT token for immediate authentication. " +
                     "The token is valid for 24 hours and should be included in the Authorization header for protected endpoints."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input - email already exists or validation failed"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Login user
     * POST /auth/login
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticates user with email and password. Returns a JWT token valid for 24 hours. " +
                     "Use this token in the Authorization header as 'Bearer {token}' for protected endpoints."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials - email or password incorrect"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Validate token
     * GET /auth/validate
     */
    @GetMapping("/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Validates a JWT token and returns user information if valid. " +
                     "This endpoint is used by other services to verify tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token is valid",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token is invalid or expired"
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(description = "JWT token in format: Bearer {token}", required = true)
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Token validation request received");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        boolean isValid = authService.validateToken(token);

        if (isValid) {
            String userId = authService.extractUserId(token);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Health check
     * GET /auth/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Returns the health status of the Auth Service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is healthy",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }
}
