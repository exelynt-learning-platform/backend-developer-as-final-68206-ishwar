package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/resources", "/resources"})
@Tag(name = "2. Resource Management", description = "Endpoints for viewing and managing bookable resources (USER: Read-only, ADMIN: Full CRUD)")
public class ResourceController {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get list of all resources", description = "Returns an array/list of all resources available for booking. Admins can pass activeOnly=false to view inactive resources.")
    @ApiResponse(responseCode = "200", description = "List of resources",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ResourceResponse.class))))
    public List<ResourceResponse> getResources(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.getResources(activeOnly);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID", description = "Fetches details of a specific resource by its ID.")
    public ResourceResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create a resource (ADMIN only)", description = "Creates a new bookable resource. Requires ADMIN role.")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a resource (ADMIN only)", description = "Updates an existing resource details. Requires ADMIN role.")
    public ResourceResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ResourceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resource (ADMIN only)", description = "Deletes a resource by its ID. Requires ADMIN role.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
