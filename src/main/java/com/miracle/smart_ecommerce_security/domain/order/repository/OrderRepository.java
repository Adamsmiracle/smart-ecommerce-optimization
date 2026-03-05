package com.miracle.smart_ecommerce_security.domain.order.repository;

import com.miracle.smart_ecommerce_security.domain.order.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for CustomerOrder domain model.
 */
@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /**
     * Find order by ID with user, orderItems, and each item's product eagerly fetched.
     * Eliminates N+1 queries when building the full order response.
     */
    @Query("SELECT DISTINCT o FROM CustomerOrder o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.id = :id")
    Optional<CustomerOrder> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Find order by order number with user, orderItems, and each item's product eagerly fetched.
     */
    @Query("SELECT DISTINCT o FROM CustomerOrder o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.orderNumber = :orderNumber")
    Optional<CustomerOrder> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);

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

    /**
     * Native SQL reporting query: top customers by total spending.
     *
     * Uses native SQL (nativeQuery = true) to leverage PostgreSQL-specific aggregation
     * and ordering that is more concise to express at the SQL level than in JPQL.
     *
     * Returns rows of: [user_id, first_name, last_name, email_address, total_orders, total_spent]
     * Only counts orders in a completed/delivered state to represent real revenue.
     *
     * @param limit maximum number of customers to return
     * @return list of Object[] rows with spending summary per customer
     */
    @Query(value = """
            SELECT
                u.id            AS user_id,
                u.first_name    AS first_name,
                u.last_name     AS last_name,
                u.email_address AS email_address,
                COUNT(o.id)     AS total_orders,
                SUM(o.total)    AS total_spent
            FROM app_user u
            INNER JOIN customer_order o ON o.user_id = u.id
            WHERE o.status IN ('delivered', 'confirmed', 'shipped', 'processing')
            GROUP BY u.id, u.first_name, u.last_name, u.email_address
            ORDER BY total_spent DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Object[]> findTopCustomersBySpending(@Param("limit") int limit);
}