package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login credentials")
public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        @NotBlank String username,

        @Schema(description = "Password", example = "Admin@123")
        @NotBlank String password
) {}
