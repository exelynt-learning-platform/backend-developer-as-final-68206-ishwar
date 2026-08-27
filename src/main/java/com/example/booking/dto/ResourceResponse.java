package com.example.booking.dto;

import com.example.booking.entity.Resource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resource details response")
public record ResourceResponse(
        @Schema(description = "Resource ID", example = "1")
        Long id,

        @Schema(description = "Name of the resource", example = "Conference Room A")
        String name,

        @Schema(description = "Type/Category of resource", example = "ROOM")
        String type,

        @Schema(description = "Description of the resource", example = "10-seat conference room with projector")
        String description,

        @Schema(description = "Hourly price in decimal", example = "500.00")
        BigDecimal pricePerHour,

        @Schema(description = "Whether the resource is active for booking", example = "true")
        boolean active
) {
    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(
                r.getId(), r.getName(), r.getType(), r.getDescription(),
                r.getPricePerHour(), r.isActive()
        );
    }
}
