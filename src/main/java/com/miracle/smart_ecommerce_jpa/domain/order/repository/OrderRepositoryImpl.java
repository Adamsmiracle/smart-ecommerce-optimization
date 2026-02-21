package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.order.mapper.CustomerOrderMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of OrderRepository.
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final CustomerOrderMapper orderMapper;

    public OrderRepositoryImpl(JdbcTemplate jdbcTemplate, CustomerOrderMapper orderRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = orderRowMapper;
    }

    @Override
    @Transactional
    public CustomerOrder save(CustomerOrder order) {
        String sql = """
            INSERT INTO customer_order (user_id, order_number, status, payment_method_id, shipping_method_id, payment_status,
                subtotal, total_amount, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;

        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.queryForObject(sql, orderMapper,
                order.getUserId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethodId(),
                order.getShippingMethodId(),
                order.getPaymentStatus(),
                order.getSubtotal(),
                order.getTotal(),
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }

    @Override
    @Transactional
    public CustomerOrder update(CustomerOrder order) {
        String sql = """
            UPDATE customer_order
            SET status = ?, payment_method_id = ?, payment_status = ?, shipping_method_id = ?,
                subtotal = ?, total_amount = ?, updated_at = ?
            WHERE id = ?
            RETURNING *
            """;

        try {
            return jdbcTemplate.queryForObject(sql, orderMapper,
                    order.getStatus(),
                    order.getPaymentMethodId(),
                    order.getPaymentStatus(),
                    order.getShippingMethodId(),
                    order.getSubtotal(),
                    order.getTotal(),
                    Timestamp.from(OffsetDateTime.now().toInstant()),
                    order.getId()
            );
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("Order", order.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findById(UUID id) {
        String sql = "SELECT * FROM customer_order WHERE id = ?";
        try {
            CustomerOrder order = jdbcTemplate.queryForObject(sql, orderMapper, id);
            return Optional.ofNullable(order);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findByOrderNumber(String orderNumber) {
        String sql = "SELECT * FROM customer_order WHERE order_number = ?";
        try {
            CustomerOrder order = jdbcTemplate.queryForObject(sql, orderMapper, orderNumber);
            return Optional.ofNullable(order);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrder> findAll() {
        String sql = "SELECT * FROM customer_order ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, orderMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrder> findAll(int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM customer_order ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, orderMapper, size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrder> findByUserId(UUID userId, int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM customer_order WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, orderMapper, userId, size, JdbcUtils.calculateOffset(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrder> findByStatus(String status, int page, int size) {
        JdbcUtils.validatePagination(page, size);
        String sql = "SELECT * FROM customer_order WHERE status = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, orderMapper, status, size, JdbcUtils.calculateOffset(page, size));
    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM customer_order WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("Order", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM customer_order WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        String sql = "SELECT COUNT(*) FROM customer_order";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM customer_order WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        String sql = "SELECT COUNT(*) FROM customer_order WHERE user_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, String status) {
        String sql = "UPDATE customer_order SET status = ? WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, status, id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("Order", id);
        }
    }

    @Override
    @Transactional
    public void updatePaymentStatus(UUID id, String paymentStatus) {
        String sql = "UPDATE customer_order SET payment_status = ? WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, paymentStatus, id);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("Order", id);
        }
    }
}
