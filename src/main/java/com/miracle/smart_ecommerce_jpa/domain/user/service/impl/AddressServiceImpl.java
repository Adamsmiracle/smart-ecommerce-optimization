package com.miracle.smart_ecommerce_jpa.domain.user.service.impl;

import com.miracle.smart_ecommerce_jpa.domain.user.entity.Address;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.AddressService;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.AddressRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of AddressService.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Create a new address for a user.
     * Cache is evicted to ensure stale address lists are refreshed.
     */
    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse createAddress(CreateAddressRequest request) {
        log.info("Creating address for user: {}", request.getUserId());

        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        Address address = Address.builder()
                .userId(request.getUserId())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .region(request.getRegion())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .addressType(request.getAddressType())
                .isDefault(false)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created with ID: {} at: {}", saved.getId(), saved.getCreatedAt());
        return mapToResponse(saved);
    }

    /**
     * Get address by ID.
     * Result is cached by ID to avoid repeated DB lookups.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ADDRESSES_CACHE, key = "'id:' + #id")
    public AddressResponse getAddressById(UUID id) {
        log.debug("Getting address by ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));
        return mapToResponse(address);
    }

    /**
     * Get all addresses - admin use only, no caching due to potentially large result set.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses() {
        log.debug("Getting all addresses");
        return addressRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get all addresses for a specific user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        log.debug("Getting addresses for user: {}", userId);
        List<AddressResponse> addresses = addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
        log.info("Found {} addresses for userId: {}", addresses.size(), userId);
        return addresses;
    }

    /**
     * Get addresses for a user filtered by type (e.g. shipping, billing).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserIdAndType(UUID userId, String addressType) {
        log.debug("Getting {} addresses for user: {}", addressType, userId);
        List<AddressResponse> addresses = addressRepository.findByUserIdAndAddressType(userId, addressType).stream()
                .map(this::mapToResponse)
                .toList();
        log.info("Found {} {} addresses for userId: {}", addresses.size(), addressType, userId);
        return addresses;
    }

    /**
     * Update an existing address.
     * Uses JPA dirty checking — no explicit save() needed after mutation.
     * Cache is evicted to prevent stale data.
     */
    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse updateAddress(UUID id, CreateAddressRequest request) {
        log.info("Updating address: {}", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));

        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setRegion(request.getRegion());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());

        log.info("Address updated successfully: {}", id);
        return mapToResponse(address);
    }

    /**
     * Set an address as the default for its user.
     * Clears any existing default first to ensure only one default per user.
     * Both operations run in the same transaction for consistency.
     */
    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse setDefaultAddress(UUID id) {
        log.info("Setting address {} as default", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));

        // Clear existing default for this user before setting the new one
        addressRepository.clearDefaultByUserId(address.getUserId());
        addressRepository.setAsDefault(id);

        address.setIsDefault(true);
        log.info("Address {} set as default for user {}", id, address.getUserId());
        return mapToResponse(address);
    }

    /**
     * Delete an address by ID.
     * Cache is evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public void deleteAddress(UUID id) {
        log.info("Deleting address: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));
        addressRepository.delete(address);
        log.info("Address deleted successfully: {}", id);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUserId())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .region(address.getRegion())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}