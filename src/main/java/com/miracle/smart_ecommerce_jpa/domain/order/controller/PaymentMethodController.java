package com.miracle.smart_ecommerce_jpa.domain.order.controller;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.PaymentMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.service.PaymentMethodService;
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
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "APIs for managing user payment methods")
@RequireRoles({"ADMIN", "CUSTOMER"})
public class PaymentMethodController {

    private final PaymentMethodService service;

    @PostMapping
    @Operation(summary = "Create payment method", description = "Create a new payment method for a user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment method created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> create(@Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req), "Payment method created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment method", description = "Update an existing payment method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment method updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> update(
            @Parameter(description = "Payment method ID") @PathVariable UUID id,
            @Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Payment method updated"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment method", description = "Retrieve a payment method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment method retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> getById(
            @Parameter(description = "Payment method ID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List user payment methods", description = "List payment methods for a specific user (paged)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paged list of payment methods retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<PageResponse<PaymentMethodResponse>>> getByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getByUserId(userId, pageable)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment method", description = "Delete a payment method by id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Payment method deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Payment method ID") @PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Payment method deleted"));
    }
}
