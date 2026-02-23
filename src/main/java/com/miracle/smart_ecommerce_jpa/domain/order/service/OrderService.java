package com.miracle.smart_ecommerce_jpa.domain.order.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.CreateOrderRequest;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.OrderResponse;
import com.miracle.smart_ecommerce_jpa.domain.order.dto.UpdateOrderRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Order operations.
 */
public interface OrderService {

    /**
     * Create a new order
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Get order by ID
     */
    OrderResponse getOrderById(UUID id);

    /**
     * Get order by order number
     */
    OrderResponse getOrderByOrderNumber(String orderNumber);

    /**
     * Get all orders with pagination
     */
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    /**
     * Get orders by user ID with pagination
     */
    PageResponse<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable);

    /**
     * Get orders by status with pagination
     */
    PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable);

    /**
     * Update order status
     */
    OrderResponse updateOrderStatus(UUID id, String status);

    /**
     * Update payment status
     */
    OrderResponse updatePaymentStatus(UUID id, String paymentStatus);

    /**
     * Cancel an order and restore stock
     */
    OrderResponse cancelOrder(UUID id);

    /**
     * Update order top-level editable fields and items
     */
    OrderResponse updateOrder(UUID id, UpdateOrderRequest request);

    /**
     * Delete an order
     */
    void deleteOrder(UUID id);

    /**
     * Count total orders
     */
    long countOrders();

    /**
     * Count orders by status
     */
    long countOrdersByStatus(String status);
}