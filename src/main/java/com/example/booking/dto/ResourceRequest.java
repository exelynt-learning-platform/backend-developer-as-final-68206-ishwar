package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Request body for creating or updating a resource")
public record ResourceRequest(
        @Schema(description = "Resource name", example = "Conference Room A")
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "Resource type (ROOM, VEHICLE, EQUIPMENT, etc.)", example = "ROOM")
        @NotBlank @Size(max = 50) String type,

        @Schema(description = "Resource description", example = "10-seat conference room with projector")
        @Size(max = 500) String description,

        @Schema(description = "Hourly rental rate in decimal", example = "500.00")
        @NotNull @DecimalMin(value = "0.01") BigDecimal pricePerHour,

        @Schema(description = "Whether the resource is available for booking", example = "true")
        Boolean active
) {}
