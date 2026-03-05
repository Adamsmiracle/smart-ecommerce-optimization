package com.miracle.smart_ecommerce_security.domain.user.service;

import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.AddressResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Get all addresses (Admin) - paginated
     */
    Page<AddressResponse> getAllAddresses(Pageable pageable);

    /**
     * Get all addresses for a user - paginated
     */
    Page<AddressResponse> getAddressesByUserId(UUID userId, Pageable pageable);

    /**
     * Get addresses by user ID and type (shipping/billing) - paginated
     */
    Page<AddressResponse> getAddressesByUserIdAndType(UUID userId, String addressType, Pageable pageable);

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
