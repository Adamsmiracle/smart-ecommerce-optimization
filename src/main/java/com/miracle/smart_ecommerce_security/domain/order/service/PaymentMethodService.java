package com.miracle.smart_ecommerce_security.domain.order.service;

import com.miracle.smart_ecommerce_security.domain.order.dto.PaymentMethodRequest;
import com.miracle.smart_ecommerce_security.domain.order.dto.PaymentMethodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodService {

    /**
     * Create a new payment method for a user
     */
    PaymentMethodResponse create(PaymentMethodRequest request);

    /**
     * Update an existing payment method
     */
    PaymentMethodResponse update(UUID id, PaymentMethodRequest request);

    /**
     * Get payment method by ID
     */
    PaymentMethodResponse getById(UUID id);

    /**
     * Get all payment methods for a user with pagination
     */
    Page<PaymentMethodResponse> getByUserId(UUID userId, Pageable pageable);

    /**
     * Get all active payment methods for a user
     */
    List<PaymentMethodResponse> getActiveByUserId(UUID userId);

    /**
     * Set a payment method as the default for a user
     */
    PaymentMethodResponse setDefault(UUID id);

    /**
     * Deactivate a payment method (soft delete)
     */
    void deactivate(UUID id);

    /**
     * Delete a payment method permanently
     */
    void delete(UUID id);
}