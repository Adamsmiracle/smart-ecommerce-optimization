package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for OrderItem operations.
 */
public interface OrderItemRepository {

    /**
     * Save a new order item
     */
    OrderItem save(OrderItem item);

    /**
     * Find order item by ID
     */
    Optional<OrderItem> findById(UUID id);

    /**
     * Find all items for an order
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Delete order item by ID
     */
    void deleteById(UUID id);

    /**
     * Delete all items for an order
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Count items for an order
     */
    long countByOrderId(UUID orderId);
}

