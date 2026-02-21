package com.miracle.smart_ecommerce_jpa.domain.order.controller;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.service.ShippingMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/shipping-methods")
@RequiredArgsConstructor
@Tag(name = "Shipping Methods", description = "APIs for managing shipping methods")
public class ShippingMethodController {

    private final ShippingMethodService service;

    @PostMapping
    @Operation(summary = "Create shipping method", description = "Create a new shipping method")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipping method created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ShippingMethodResponse> create(@Valid @RequestBody ShippingMethodRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shipping method", description = "Update an existing shipping method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipping method updated successfully"),
            @ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    public ResponseEntity<ShippingMethodResponse> update(
            @Parameter(description = "Shipping method ID") @PathVariable UUID id,
            @Valid @RequestBody ShippingMethodRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shipping method", description = "Retrieve a shipping method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipping method retrieved"),
            @ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    public ResponseEntity<ShippingMethodResponse> getById(
            @Parameter(description = "Shipping method ID") @PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "List shipping methods", description = "Paged list of shipping methods")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paged list retrieved")
    })
    public ResponseEntity<PageResponse<ShippingMethodResponse>> list(@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                                                                       @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete shipping method", description = "Delete a shipping method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Shipping method deleted"),
            @ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Shipping method ID") @PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
