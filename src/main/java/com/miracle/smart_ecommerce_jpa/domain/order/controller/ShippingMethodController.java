package com.miracle.smart_ecommerce_jpa.domain.order.controller;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.service.ShippingMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/shipping-methods")
@RequiredArgsConstructor
@Tag(name = "Shipping Methods", description = "APIs for managing shipping methods")
@RequireRoles({"ADMIN", "CUSTOMER"})
public class ShippingMethodController {

    private final ShippingMethodService service;

    @PostMapping
    @Operation(summary = "Create shipping method", description = "Create a new shipping method")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipping method created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @RequireRoles({"ADMIN"})
    public ResponseEntity<ApiResponse<ShippingMethodResponse>> create(@Valid @RequestBody ShippingMethodRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req), "Shipping method created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shipping method", description = "Update an existing shipping method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipping method updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    @RequireRoles({"ADMIN"})
    public ResponseEntity<ApiResponse<ShippingMethodResponse>> update(
            @Parameter(description = "Shipping method ID") @PathVariable UUID id,
            @Valid @RequestBody ShippingMethodRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Shipping method updated"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shipping method", description = "Retrieve a shipping method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipping method retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    public ResponseEntity<ApiResponse<ShippingMethodResponse>> getById(
            @Parameter(description = "Shipping method ID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List shipping methods", description = "Paged list of shipping methods")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paged list retrieved")
    })
    public ResponseEntity<ApiResponse<PageResponse<ShippingMethodResponse>>> list(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(pageable)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete shipping method", description = "Delete a shipping method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Shipping method deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipping method not found")
    })
    @RequireRoles({"ADMIN"})
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Shipping method ID") @PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Shipping method deleted"));
    }
}
