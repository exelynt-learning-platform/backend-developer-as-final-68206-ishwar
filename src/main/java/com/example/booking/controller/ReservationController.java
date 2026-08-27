package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/reservations", "/reservations"})
@Tag(name = "3. User Reservations", description = "Endpoints for authenticated users to create and manage their own reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a reservation (USER)", description = "Creates a new reservation for the currently authenticated user. Price is calculated automatically.")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Get current user's reservations", description = "Returns a paginated list of reservations belonging only to the authenticated user. Supports filtering by status, minPrice, and maxPrice.")
    public Page<ReservationResponse> myReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {

        return service.search(
                authentication.getName(), false, status, minPrice, maxPrice,
                page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID (Own)", description = "Fetches a specific reservation belonging to the authenticated user.")
    public ReservationResponse getOne(
            @PathVariable Long id,
            Authentication authentication) {

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return service.getByIdForCurrentUser(
                id, authentication.getName(), admin);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update own reservation", description = "Updates the resource, start time, and end time for the user's own reservation.")
    public ReservationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request,
            Authentication authentication) {

        return service.updateOwn(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel own reservation", description = "Cancels the user's own reservation (changes status to CANCELLED).")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        service.cancelOwn(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
