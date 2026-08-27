package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/admin", "/admin"})
@Tag(name = "4. Admin Reservation Management", description = "Endpoints for administrators to manage all reservations across the system")
public class AdminController {

    private final ReservationService reservationService;

    public AdminController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @Operation(summary = "Create reservation for any user (ADMIN only)", description = "Creates a reservation assigning any target userId and resourceId. Requires ADMIN role.")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody AdminReservationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createForAdmin(request));
    }

    @GetMapping("/reservations")
    @Operation(summary = "Get all reservations across all users (ADMIN only)", description = "Returns a paginated list of all system reservations. Supports status, minPrice, maxPrice filtering and sorting.")
    public Page<ReservationResponse> getAllReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return reservationService.search(
                null, true, status, minPrice, maxPrice,
                page, size, sortBy, direction);
    }

    @GetMapping("/reservations/{id}")
    @Operation(summary = "Get any reservation by ID (ADMIN only)", description = "Fetches details of any reservation by its ID.")
    public ReservationResponse getReservation(@PathVariable Long id) {
        return reservationService.getByIdForCurrentUser(id, null, true);
    }

    @PutMapping("/reservations/{id}")
    @Operation(summary = "Update reservation details (ADMIN only)", description = "Updates user, resource, and time window for any reservation.")
    public ReservationResponse updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody AdminReservationRequest request) {

        return reservationService.updateAsAdmin(id, request);
    }

    @PutMapping("/reservations/{id}/status")
    @Operation(summary = "Update reservation status (ADMIN only)", description = "Changes the status of a reservation to PENDING, CONFIRMED, or CANCELLED.")
    public ReservationResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusRequest request) {

        return reservationService.updateStatus(id, request.status());
    }

    @DeleteMapping("/reservations/{id}")
    @Operation(summary = "Delete reservation (ADMIN only)", description = "Permanently deletes a reservation from the system.")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
