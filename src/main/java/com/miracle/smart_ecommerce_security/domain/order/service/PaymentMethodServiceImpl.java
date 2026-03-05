package com.miracle.smart_ecommerce_security.domain.order.service;

import com.miracle.smart_ecommerce_security.domain.order.dto.PaymentMethodRequest;
import com.miracle.smart_ecommerce_security.domain.order.dto.PaymentMethodResponse;
import com.miracle.smart_ecommerce_security.domain.order.entity.PaymentMethod;
import com.miracle.smart_ecommerce_security.domain.order.repository.PaymentMethodRepository;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_security.config.CacheConfig.*;

/**
 * Implementation of PaymentMethodService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance
 * - Write operations use default REQUIRED propagation
 * - setDefault clears all existing defaults and sets the new one atomically
 *
 * Cache strategy:
 * - Payment methods cached by ID
 * - All entries evicted on create, update, delete, deactivate, and setDefault
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - DataIntegrityViolationException caught as safety net for DB constraint violations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository repository;
    private final UserRepository userRepository;

    /**
     * Create a new payment method for a user.
     * Validates user existence before saving.
     * New payment methods are active but not default by default.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = PAYMENT_METHODS_CACHE, key = "'id:' + #result.id") },
            evict = { @CacheEvict(value = PAYMENT_METHODS_CACHE, allEntries = true) }
    )
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        log.info("Creating payment method for user: {}", request.getUserId());

        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        try {
            PaymentMethod pm = PaymentMethod.builder()
                    .user(User.builder().id(request.getUserId()).build())
                    .paymentType(request.getPaymentType())
                    .provider(request.getProvider())
                    .accountNumber(request.getAccountNumber())
                    .expiryDate(request.getExpiryDate())
                    .isActive(true)
                    .isDefault(false)
                    .build();

            PaymentMethod saved = repository.save(pm);
            log.info("Payment method created with ID: {}", saved.getId());
            return toResponse(saved);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating payment method for user: {}", request.getUserId(), e);
            throw new DataIntegrityViolationException("Failed to create payment method: " + e.getMessage());
        }
    }

    /**
     * Update an existing payment method.
     * Uses JPA dirty checking — no explicit save() needed.
     * Cache evicted after update.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = PAYMENT_METHODS_CACHE, key = "'id:' + #id") },
            evict = { @CacheEvict(value = PAYMENT_METHODS_CACHE, allEntries = true) }
    )
    public PaymentMethodResponse update(UUID id, PaymentMethodRequest request) {
        log.info("Updating payment method: {}", id);

        PaymentMethod pm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("PaymentMethod", id));

        pm.setPaymentType(request.getPaymentType());
        pm.setProvider(request.getProvider());
        pm.setAccountNumber(request.getAccountNumber());
        pm.setExpiryDate(request.getExpiryDate());

        log.info("Payment method updated successfully: {}", id);
        return toResponse(pm);
    }

    /**
     * Get payment method by ID.
     * Result cached by ID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PAYMENT_METHODS_CACHE, key = "'id:' + #id")
    public PaymentMethodResponse getById(UUID id) {
        log.debug("Getting payment method by ID: {}", id);
        PaymentMethod pm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("PaymentMethod", id));
        return toResponse(pm);
    }

    /**
     * Get all payment methods for a user with pagination.
     * Cached by userId + page + size for repeated browsing.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PAYMENT_METHODS_CACHE,
            key = "'user:' + #userId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public Page<PaymentMethodResponse> getByUserId(UUID userId, Pageable pageable) {
        log.debug("Getting payment methods for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        return repository.findByUser_Id(userId, pageable).map(this::toResponse);
    }

    /**
     * Get all active payment methods for a user.
     * Cached by userId — frequently called during checkout.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PAYMENT_METHODS_CACHE, key = "'user:active:' + #userId")
    public List<PaymentMethodResponse> getActiveByUserId(UUID userId) {
        log.debug("Getting active payment methods for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        return repository.findByUser_IdAndIsActiveTrue(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Set a payment method as the default for its user.
     * Updated entry cached by ID; all list entries evicted.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = PAYMENT_METHODS_CACHE, key = "'id:' + #id") },
            evict = { @CacheEvict(value = PAYMENT_METHODS_CACHE, allEntries = true) }
    )
    public PaymentMethodResponse setDefault(UUID id) {
        log.info("Setting payment method {} as default", id);

        PaymentMethod pm = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("PaymentMethod", id));

        repository.clearDefaultByUserId(pm.getUser().getId());
        repository.setAsDefault(id);

        pm.setIsDefault(true);
        log.info("Payment method {} set as default for user {}", id, pm.getUser().getId());
        return toResponse(pm);
    }

    /**
     * Deactivate a payment method (soft delete).
     * Cache evicted after deactivation.
     */
    @Override
    @Transactional
    @CacheEvict(value = PAYMENT_METHODS_CACHE, allEntries = true)
    public void deactivate(UUID id) {
        log.info("Deactivating payment method: {}", id);

        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.forResource("PaymentMethod", id);
        }

        repository.setActiveStatus(id, false);
        log.info("Payment method deactivated: {}", id);
    }

    /**
     * Permanently delete a payment method.
     * Cache evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = PAYMENT_METHODS_CACHE, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting payment method: {}", id);

        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.forResource("PaymentMethod", id);
        }

        try {
            repository.deleteById(id);
            log.info("Payment method deleted: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete payment method {} — it may be referenced by existing orders", id, e);
            throw new DataIntegrityViolationException("Cannot delete payment method as it is referenced by existing orders.");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private PaymentMethodResponse toResponse(PaymentMethod pm) {
        return PaymentMethodResponse.builder()
                .id(pm.getId())
                .userId(pm.getUser() != null ? pm.getUser().getId() : null)
                .paymentType(pm.getPaymentType())
                .provider(pm.getProvider())
                .maskedAccount(pm.getMaskedAccountNumber())
                .expiryDate(pm.getExpiryDate())
                .isDefault(pm.getIsDefault())
                .isActive(pm.getIsActive())
                .createdAt(pm.getCreatedAt())
                .build();
    }
}