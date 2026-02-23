package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.repository.ShippingMethodRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of ShippingMethodService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance
 * - Write operations use default REQUIRED propagation
 * - Dirty checking handles updates without explicit save()
 *
 * Cache strategy:
 * - Shipping methods cached by ID
 * - All entries evicted on create, update, delete, and deactivate
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - DataIntegrityViolationException caught as safety net for DB constraint violations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingMethodServiceImpl implements ShippingMethodService {

    private final ShippingMethodRepository repository;

    /**
     * Create a new shipping method.
     * Active by default on creation.
     */
    @Override
    @Transactional
    @CacheEvict(value = SHIPPING_METHODS_CACHE, allEntries = true)
    public ShippingMethodResponse create(ShippingMethodRequest request) {
        log.info("Creating shipping method: {}", request.getName());

        try {
            ShippingMethod sm = ShippingMethod.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .estimatedDays(request.getEstimatedDays())
                    .isActive(true)
                    .build();

            ShippingMethod saved = repository.save(sm);
            log.info("Shipping method created with ID: {}", saved.getId());
            return toResponse(saved);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating shipping method: {}", request.getName(), e);
            throw new DataIntegrityViolationException("Failed to create shipping method: " + e.getMessage());
        }
    }

    /**
     * Update an existing shipping method.
     * Uses JPA dirty checking — no explicit save() needed.
     * Cache evicted after update.
     */
    @Override
    @Transactional
    @CacheEvict(value = SHIPPING_METHODS_CACHE, allEntries = true)
    public ShippingMethodResponse update(UUID id, ShippingMethodRequest request) {
        log.info("Updating shipping method: {}", id);

        ShippingMethod sm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", id));

        sm.setName(request.getName());
        sm.setDescription(request.getDescription());
        sm.setPrice(request.getPrice());
        sm.setEstimatedDays(request.getEstimatedDays());

        log.info("Shipping method updated successfully: {}", id);
        return toResponse(sm);
    }

    /**
     * Get shipping method by ID.
     * Result cached by ID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = SHIPPING_METHODS_CACHE, key = "'id:' + #id")
    public ShippingMethodResponse getById(UUID id) {
        log.debug("Getting shipping method by ID: {}", id);
        ShippingMethod sm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("ShippingMethod", id));
        return toResponse(sm);
    }

    /**
     * Get all shipping methods with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShippingMethodResponse> getAll(Pageable pageable) {
        log.debug("Getting all shipping methods - pageable: {}", pageable);

        Page<ShippingMethod> page = repository.findAll(pageable);
        List<ShippingMethodResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
    }

    /**
     * Get all active shipping methods.
     * Cached for fast access since active methods are frequently queried during checkout.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = SHIPPING_METHODS_CACHE, key = "'active'")
    public List<ShippingMethodResponse> getActive() {
        log.debug("Getting all active shipping methods");
        return repository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Deactivate a shipping method (soft delete).
     * Cache evicted after deactivation.
     */
    @Override
    @Transactional
    @CacheEvict(value = SHIPPING_METHODS_CACHE, allEntries = true)
    public void deactivate(UUID id) {
        log.info("Deactivating shipping method: {}", id);

        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.forResource("ShippingMethod", id);
        }

        repository.setActiveStatus(id, false);
        log.info("Shipping method deactivated: {}", id);
    }

    /**
     * Permanently delete a shipping method.
     * Cache evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = SHIPPING_METHODS_CACHE, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting shipping method: {}", id);

        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.forResource("ShippingMethod", id);
        }

        try {
            repository.deleteById(id);
            log.info("Shipping method deleted: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete shipping method {} — it may be referenced by existing orders", id, e);
            throw new DataIntegrityViolationException("Cannot delete shipping method as it is referenced by existing orders.");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ShippingMethodResponse toResponse(ShippingMethod sm) {
        return ShippingMethodResponse.builder()
                .id(sm.getId())
                .name(sm.getName())
                .description(sm.getDescription())
                .price(sm.getPrice())
                .estimatedDays(sm.getEstimatedDays())
                .isActive(sm.getIsActive())
                .createdAt(sm.getCreatedAt())
                .build();
    }
}