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
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse createAddress(CreateAddressRequest request) {
        log.info("Creating address for user: {}", request.getUserId());

        // Verify user exists
        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        // If this is set as default, unset other defaults for this user and type
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUserAndType(request.getUserId(), request.getAddressType());
        }

        Address address = Address.builder()
                .userId(request.getUserId())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .region(request.getRegion())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .addressType(request.getAddressType())
                .build();

        Address savedAddress = addressRepository.save(address);
        log.info("Address created successfully with ID: {} and createdAt: {}", savedAddress.getId(), savedAddress.getCreatedAt());

        return mapToResponse(savedAddress);
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
    public List<AddressResponse> getAllAddresses() {
        log.debug("Getting all addresses");
        return addressRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        log.debug("Getting addresses for user: {}", userId);
        List<AddressResponse> addresses = addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        log.info("Found {} addresses for userId: {}", addresses.size(), userId);
        return addresses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserIdAndType(UUID userId, String addressType) {
        log.debug("Getting {} addresses for user: {}", addressType, userId);
        List<AddressResponse> addresses = addressRepository.findByUserIdAndType(userId, addressType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        log.info("Found {} {} addresses for userId: {}", addresses.size(), addressType, userId);
        return addresses;
    }



    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse updateAddress(UUID id, CreateAddressRequest request) {
        log.info("Updating address: {}", id);

        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));


        existingAddress.setAddressLine(request.getAddressLine());
        existingAddress.setCity(request.getCity());
        existingAddress.setRegion(request.getRegion());
        existingAddress.setCountry(request.getCountry());
        existingAddress.setPostalCode(request.getPostalCode());
        existingAddress.setAddressType(request.getAddressType());

        Address updatedAddress = addressRepository.update(existingAddress);
        log.info("Address updated successfully: {}", id);

        return mapToResponse(updatedAddress);
    }

    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public AddressResponse setDefaultAddress(UUID id) {
        log.info("Setting address {} as default", id);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Address", id));

        // Clear other defaults for this user and type
        addressRepository.clearDefaultForUserAndType(address.getUserId(), address.getAddressType());

        // Set this address as default
        Address updatedAddress = addressRepository.update(address);

        log.info("Default address set successfully: {}", id);
        return mapToResponse(updatedAddress);
    }

    @Override
    @Transactional
    @CacheEvict(value = ADDRESSES_CACHE, allEntries = true)
    public void deleteAddress(UUID id) {
        log.info("Deleting address: {}", id);
        if (!addressRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Address", id);
        }
        addressRepository.deleteById(id);
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
                // createdAt is already an OffsetDateTime in BaseModel; guard against null
                .createdAt(address.getCreatedAt())
                .build();
    }
}
