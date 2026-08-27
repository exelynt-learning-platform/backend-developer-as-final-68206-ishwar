package com.example.booking.dto;

import com.example.booking.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Reservation details response")
public record ReservationResponse(
        @Schema(description = "Reservation ID", example = "1")
        Long id,

        @Schema(description = "Resource ID", example = "1")
        Long resourceId,

        @Schema(description = "Resource Name", example = "Conference Room A")
        String resourceName,

        @Schema(description = "User ID", example = "2")
        Long userId,

        @Schema(description = "Username who booked the resource", example = "user")
        String username,

        @Schema(description = "Booking start time", example = "2026-09-10T10:00:00")
        LocalDateTime startTime,

        @Schema(description = "Booking end time", example = "2026-09-10T12:00:00")
        LocalDateTime endTime,

        @Schema(description = "Calculated total price in decimal", example = "1000.00")
        BigDecimal price,

        @Schema(description = "Reservation status (PENDING, CONFIRMED, CANCELLED)", example = "PENDING")
        String status
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getResource().getId(),
                r.getResource().getName(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getStartTime(),
                r.getEndTime(),
                r.getPrice(),
                r.getStatus().name()
        );
    }
}
