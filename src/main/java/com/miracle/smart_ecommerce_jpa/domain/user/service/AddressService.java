package com.miracle.smart_ecommerce_jpa.domain.user.service;

import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Address operations.
 */
public interface AddressService {

    /**
     * Create a new address
     */
    AddressResponse createAddress(CreateAddressRequest request);

    /**
     * Get address by ID
     */
    AddressResponse getAddressById(UUID id);

    /**
     * Get all addresses (Admin)
     */
    List<AddressResponse> getAllAddresses();

    /**
     * Get all addresses for a user
     */
    List<AddressResponse> getAddressesByUserId(UUID userId);

    /**
     * Get addresses by user ID and type (shipping/billing)
     */
    List<AddressResponse> getAddressesByUserIdAndType(UUID userId, String addressType);

    /**
     * Update an address
     */
    AddressResponse updateAddress(UUID id, CreateAddressRequest request);

    /**
     * Set an address as default
     */
    AddressResponse setDefaultAddress(UUID id);

    /**
     * Delete an address
     */
    void deleteAddress(UUID id);
}

