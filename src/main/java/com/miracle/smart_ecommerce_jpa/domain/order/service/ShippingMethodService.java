package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.ShippingMethodResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;

import java.util.UUID;

public interface ShippingMethodService {
    ShippingMethodResponse create(ShippingMethodRequest request);
    ShippingMethodResponse update(UUID id, ShippingMethodRequest request);
    ShippingMethodResponse getById(UUID id);
    PageResponse<ShippingMethodResponse> getAll(int page, int size);
    void delete(UUID id);
}

