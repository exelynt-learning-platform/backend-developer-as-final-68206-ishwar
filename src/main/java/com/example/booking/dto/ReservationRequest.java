package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Request body for creating a reservation")
public record ReservationRequest(
        @Schema(description = "Resource ID to reserve", example = "1")
        @NotNull Long resourceId,

        @Schema(description = "Booking start time (must be in future)", example = "2026-09-10T10:00:00")
        @NotNull LocalDateTime startTime,

        @Schema(description = "Booking end time (must be after start time)", example = "2026-09-10T12:00:00")
        @NotNull LocalDateTime endTime
) {}
