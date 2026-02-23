package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ShippingMethodService {

    /**
     * Create a new shipping method
     */
    ShippingMethodResponse create(ShippingMethodRequest request);

    /**
     * Update an existing shipping method
     */
    ShippingMethodResponse update(UUID id, ShippingMethodRequest request);

    /**
     * Get shipping method by ID
     */
    ShippingMethodResponse getById(UUID id);

    /**
     * Get all shipping methods with pagination
     */
    PageResponse<ShippingMethodResponse> getAll(Pageable pageable);

    /**
     * Get all active shipping methods
     */
    List<ShippingMethodResponse> getActive();

    /**
     * Deactivate a shipping method (soft delete)
     */
    void deactivate(UUID id);

    /**
     * Permanently delete a shipping method
     */
    void delete(UUID id);
}