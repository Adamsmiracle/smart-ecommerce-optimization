package com.miracle.smart_ecommerce_jpa.domain.user.repository;

import com.miracle.smart_ecommerce_jpa.domain.user.entity.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Address operations.
 */
public interface AddressRepository {

    /**
     * Save a new address
     */
    Address save(Address address);

    /**
     * Update an existing address
     */
    Address update(Address address);

    /**
     * Find address by ID
     */
    Optional<Address> findById(UUID id);

    /**
     * Find all addresses
     */
    List<Address> findAll();

    /**
     * Find all addresses for a user
     */
    List<Address> findByUserId(UUID userId);

    /**
     * Find addresses by user ID and type
     */
    List<Address> findByUserIdAndType(UUID userId, String addressType);

    /**
     * Find default address for a user
     */
    Optional<Address> findDefaultByUserId(UUID userId);

    /**
     * Check if address exists by ID
     */
    boolean existsById(UUID id);

    /**
     * Delete address by ID
     */
    void deleteById(UUID id);

    /**
     * Clear default flag for all addresses of a user and type
     */
    void clearDefaultForUserAndType(UUID userId, String addressType);
}

