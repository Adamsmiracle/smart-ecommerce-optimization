package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository interface for OrderItem domain model.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Find all items for an order
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Delete all items for an order
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Count items for an order
     */
    long countByOrderId(UUID orderId);
}