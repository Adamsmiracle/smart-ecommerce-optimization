package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for CustomerOrder domain model.
 */
@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /**
     * Find order by order number
     */
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    /**
     * Find orders by user ID with pagination
     */
    Page<CustomerOrder> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find orders by status with pagination
     */
    Page<CustomerOrder> findByStatus(String status, Pageable pageable);

    /**
     * Find orders by user ID and status with pagination
     */
    Page<CustomerOrder> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

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
    @Modifying
    @Query("UPDATE CustomerOrder o SET o.status = :status WHERE o.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    /**
     * Update payment status
     */
    @Modifying
    @Query("UPDATE CustomerOrder o SET o.paymentStatus = :paymentStatus WHERE o.id = :id")
    void updatePaymentStatus(@Param("id") UUID id, @Param("paymentStatus") String paymentStatus);
}