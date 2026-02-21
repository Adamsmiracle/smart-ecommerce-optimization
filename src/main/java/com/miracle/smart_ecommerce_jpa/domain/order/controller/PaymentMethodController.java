package com.miracle.smart_ecommerce_jpa.domain.order.controller;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.service.PaymentMethodService;
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
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "APIs for managing user payment methods")
public class PaymentMethodController {

    private final PaymentMethodService service;

    @PostMapping
    @Operation(summary = "Create payment method", description = "Create a new payment method for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment method created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment method", description = "Update an existing payment method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment method updated successfully"),
            @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<PaymentMethodResponse> update(
            @Parameter(description = "Payment method ID") @PathVariable UUID id,
            @Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment method", description = "Retrieve a payment method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment method retrieved"),
            @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<PaymentMethodResponse> getById(
            @Parameter(description = "Payment method ID") @PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List user payment methods", description = "List payment methods for a specific user (paged)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paged list of payment methods retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<PageResponse<PaymentMethodResponse>> getByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getByUserId(userId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment method", description = "Delete a payment method by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment method deleted"),
            @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Payment method ID") @PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
