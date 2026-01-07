package com.ticketblitz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(
        description = "User email address (must be unique)",
        example = "john@example.com",
        required = true
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(
        description = "User password (minimum 6 characters)",
        example = "password123",
        required = true,
        minLength = 6
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Schema(
        description = "User full name",
        example = "John Doe",
        required = true
    )
    @NotBlank(message = "Name is required")
    private String name;
}
