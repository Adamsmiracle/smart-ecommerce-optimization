package com.miracle.smart_ecommerce_security.domain.user.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_security.domain.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Address management (shipping and billing addresses).
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "Address management APIs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
public class AddressController {

    private final AddressService addressService;


    @PostMapping
    @Operation(summary = "Create a new address", description = "Creates a new shipping or billing address")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.createAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(address, "Address created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Retrieves an address by its unique ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @Parameter(description = "Address ID") @PathVariable UUID id) {
        AddressResponse address = addressService.getAddressById(id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @GetMapping
    @Operation(summary = "Get all addresses", description = "Retrieves all addresses in the system (Admin) - paginated")
    public ResponseEntity<ApiResponse<Page<AddressResponse>>> getAllAddresses(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<AddressResponse> addresses = addressService.getAllAddresses(pageable);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user addresses", description = "Retrieves all addresses for a specific user - paginated")
    public ResponseEntity<ApiResponse<Page<AddressResponse>>> getAddressesByUserId(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AddressResponse> addresses = addressService.getAddressesByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/user/{userId}/shipping")
    @Operation(summary = "Get user shipping addresses", description = "Retrieves all shipping addresses for a user - paginated")
    public ResponseEntity<ApiResponse<Page<AddressResponse>>> getShippingAddresses(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AddressResponse> addresses = addressService.getAddressesByUserIdAndType(userId, "shipping", pageable);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/user/{userId}/billing")
    @Operation(summary = "Get user billing addresses", description = "Retrieves all billing addresses for a user - paginated")
    public ResponseEntity<ApiResponse<Page<AddressResponse>>> getBillingAddresses(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AddressResponse> addresses = addressService.getAddressesByUserIdAndType(userId, "billing", pageable);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update address", description = "Updates an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @Parameter(description = "Address ID") @PathVariable UUID id,
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.updateAddress(id, request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address updated successfully"));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address", description = "Deletes an address by ID")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "Address ID") @PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }
}

