package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with JWT token")
public record AuthResponse(
        @Schema(description = "JWT Bearer access token", example = "eyJhbGciOiJIUzUxMiJ9...")
        String token,

        @Schema(description = "Authenticated username", example = "admin")
        String username,

        @Schema(description = "User role", example = "ADMIN")
        String role
) {}
