package com.miracle.smart_ecommerce_security.domain.order.repository;

import com.miracle.smart_ecommerce_security.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
     * Find all items for an order with product eagerly fetched to avoid N+1 queries.
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") UUID orderId);

    /**
     * Find all items for a collection of order IDs with products eagerly fetched.
     * Used for batch-loading items across multiple orders to eliminate N+1 queries.
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIdInWithProduct(@Param("orderIds") Collection<UUID> orderIds);

    /**
     * Delete all items for an order
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Count items for an order
     */
    long countByOrderId(UUID orderId);
}