package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Request body for updating a reservation")
public record ReservationUpdateRequest(
        @Schema(description = "Resource ID", example = "1")
        @NotNull Long resourceId,

        @Schema(description = "Updated start time", example = "2026-09-10T14:00:00")
        @NotNull LocalDateTime startTime,

        @Schema(description = "Updated end time", example = "2026-09-10T16:00:00")
        @NotNull LocalDateTime endTime
) {}
