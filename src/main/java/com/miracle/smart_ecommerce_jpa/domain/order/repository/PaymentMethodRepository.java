package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository {
    PaymentMethod save(PaymentMethod pm);
    PaymentMethod update(PaymentMethod pm);
    Optional<PaymentMethod> findById(UUID id);
    List<PaymentMethod> findByUserId(UUID userId, int page, int size);
    void deleteById(UUID id);
}

