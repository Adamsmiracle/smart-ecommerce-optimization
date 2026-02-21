package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingMethodRepository {
    ShippingMethod save(ShippingMethod shippingMethod);
    ShippingMethod update(ShippingMethod shippingMethod);
    Optional<ShippingMethod> findById(UUID id);
    List<ShippingMethod> findAll(int page, int size);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}

