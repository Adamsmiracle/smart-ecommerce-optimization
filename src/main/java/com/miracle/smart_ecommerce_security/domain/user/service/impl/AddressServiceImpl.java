package com.miracle.smart_ecommerce_security.domain.user.service.impl;

import com.miracle.smart_ecommerce_security.domain.user.entity.Address;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_security.domain.user.repository.AddressRepository;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.domain.user.service.AddressService;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.miracle.smart_ecommerce_security.config.CacheConfig.*;

/**
 * Implementation of AddressService using entity relationships instead of hybrid IDs.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = ADDRESSES_CACHE, key = "'id:' + #result.id") },
            evict = { @CacheEvict(value = ADDRESSES_CACHE, allEntries = true) }
    )
    public AddressResponse createAddress(CreateAddressRequest request) {
        log.info("Creating address for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", request.getUserId()));

        Address address = Address.builder()
                .user(user) // Set User entity instead of userId
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ADDRESSES_CACHE, key = "'id:' + #id")
    public AddressResponse getAddressById(UUID id) {
        log.debug("Getting address by ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));
        return mapToResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ADDRESSES_CACHE,
            key = "'all:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort")
    public Page<AddressResponse> getAllAddresses(Pageable pageable) {
        log.debug("Getting all addresses - pageable: {}", pageable);
        return addressRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ADDRESSES_CACHE,
            key = "'user:' + #userId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<AddressResponse> getAddressesByUserId(UUID userId, Pageable pageable) {
        log.debug("Getting addresses for user: {} - pageable: {}", userId, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));
        return addressRepository.findByUser(user, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ADDRESSES_CACHE,
            key = "'user:' + #userId + ':type:' + #addressType + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<AddressResponse> getAddressesByUserIdAndType(UUID userId, String addressType, Pageable pageable) {
        log.debug("Getting {} addresses for user: {} - pageable: {}", addressType, userId, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));
        return addressRepository.findByUserAndAddressType(user, addressType, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = ADDRESSES_CACHE, key = "'id:' + #id") },
            evict = { @CacheEvict(value = ADDRESSES_CACHE, allEntries = true) }
    )
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

    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = ADDRESSES_CACHE, key = "'id:' + #id") },
            evict = { @CacheEvict(value = ADDRESSES_CACHE, allEntries = true) }
    )
    public AddressResponse setDefaultAddress(UUID id) {
        log.info("Setting address {} as default", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));

        // Clear existing default for this user before setting the new one
        addressRepository.clearDefaultByUser(address.getUser());
        address.setIsDefault(true);

        log.info("Address {} set as default for user {}", id, address.getUser().getId());
        return mapToResponse(address);
    }

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
                .userId(address.getUser() != null ? address.getUser().getId() : null)
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