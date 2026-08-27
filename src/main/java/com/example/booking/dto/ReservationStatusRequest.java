package com.example.booking.dto;

import com.example.booking.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for updating reservation status")
public record ReservationStatusRequest(
        @Schema(description = "New reservation status", example = "CONFIRMED")
        @NotNull ReservationStatus status
) {}
