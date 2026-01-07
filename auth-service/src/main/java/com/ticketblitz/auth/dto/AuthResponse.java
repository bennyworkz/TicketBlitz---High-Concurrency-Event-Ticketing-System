package com.ticketblitz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with JWT token")
public class AuthResponse {

    @Schema(
        description = "JWT access token (valid for 24 hours)",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;
    
    @Schema(
        description = "Token type (always Bearer)",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    @Builder.Default
    private String tokenType = "Bearer";
    
    @Schema(
        description = "Token expiration time in seconds",
        example = "86400"
    )
    private Long expiresIn;  // Seconds
    
    @Schema(
        description = "User ID",
        example = "user-123"
    )
    private String userId;
    
    @Schema(
        description = "User email address",
        example = "john@example.com"
    )
    private String email;
    
    @Schema(
        description = "User full name",
        example = "John Doe"
    )
    private String name;
    
    @Schema(
        description = "User role",
        example = "USER"
    )
    private String role;
}
