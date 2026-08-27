package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Admin request body for creating a reservation for any user")
public record AdminReservationRequest(
        @Schema(description = "Target User ID", example = "2")
        @NotNull Long userId,

        @Schema(description = "Resource ID", example = "1")
        @NotNull Long resourceId,

        @Schema(description = "Booking start time", example = "2026-09-10T10:00:00")
        @NotNull LocalDateTime startTime,

        @Schema(description = "Booking end time", example = "2026-09-10T12:00:00")
        @NotNull LocalDateTime endTime
) {}
