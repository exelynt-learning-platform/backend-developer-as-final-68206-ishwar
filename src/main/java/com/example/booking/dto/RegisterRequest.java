package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Unique username", example = "newuser")
        @NotBlank @Size(min = 3, max = 100) String username,

        @Schema(description = "Password (minimum 6 characters)", example = "Password@123")
        @NotBlank @Size(min = 6, max = 100) String password
) {}
