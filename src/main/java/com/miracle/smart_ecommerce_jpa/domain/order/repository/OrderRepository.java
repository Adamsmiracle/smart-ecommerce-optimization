package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CustomerOrder domain model.
 * Defines data access operations for orders.
 */
public interface OrderRepository {

    /**
     * Save a new order
     */
    CustomerOrder save(CustomerOrder order);

    /**
     * Update an existing order
     */
    CustomerOrder update(CustomerOrder order);

    /**
     * Find order by ID
     */
    Optional<CustomerOrder> findById(UUID id);

    /**
     * Find order by order number
     */
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    /**
     * Find all orders
     */
    List<CustomerOrder> findAll();

    /**
     * Find all orders with pagination
     */
    List<CustomerOrder> findAll(int page, int size);

    /**
     * Find orders by user ID
     */
    List<CustomerOrder> findByUserId(UUID userId, int page, int size);

    /**
     * Find orders by status
     */
    List<CustomerOrder> findByStatus(String status, int page, int size);



    /**
     * Delete order by ID
     */
    void deleteById(UUID id);

    /**
     * Check if order exists by ID
     */
    boolean existsById(UUID id);

    /**
     * Count total orders
     */
    long count();

    /**
     * Count orders by status
     */
    long countByStatus(String status);

    /**
     * Count orders by user
     */
    long countByUserId(UUID userId);

    /**
     * Update order status
     */
    void updateStatus(UUID id, String status);

    /**
     * Update payment status
     */
    void updatePaymentStatus(UUID id, String paymentStatus);
}

