package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of OrderItemRepository.
 */

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<OrderItem> orderItemRowMapper;

    public OrderItemRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderItemRowMapper = new OrderItemRowMapper();
    }

    @Override
    @Transactional
    public OrderItem save(OrderItem item) {
        String sql = """
            INSERT INTO order_item (order_id, product_id, unit_price, quantity)
            VALUES (?, ?, ?, ?)
            RETURNING id, order_id, product_id, unit_price, quantity
            """;

        return jdbcTemplate.queryForObject(sql, orderItemRowMapper,
                item.getOrderId(),
                item.getProductId(),
                item.getUnitPrice(),
                item.getQuantity()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderItem> findById(UUID id) {
        String sql = "SELECT * FROM order_item WHERE id = ?";
        try {
            OrderItem item = jdbcTemplate.queryForObject(sql, orderItemRowMapper, id);
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> findByOrderId(UUID orderId) {
        String sql = "SELECT * FROM order_item WHERE order_id = ?";
        return jdbcTemplate.query(sql, orderItemRowMapper, orderId);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM order_item WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    @Transactional
    public void deleteByOrderId(UUID orderId) {
        String sql = "DELETE FROM order_item WHERE order_id = ?";
        jdbcTemplate.update(sql, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrderId(UUID orderId) {
        String sql = "SELECT COUNT(*) FROM order_item WHERE order_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, orderId);
        return count != null ? count : 0;
    }

    /**
     * RowMapper for OrderItem
     */
    private static class OrderItemRowMapper implements RowMapper<OrderItem> {
        @Override
        public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return OrderItem.builder()
                    .id(rs.getObject("id", UUID.class))
                    .orderId(rs.getObject("order_id", UUID.class))
                    .productId(rs.getObject("product_id", UUID.class))
                    .unitPrice(rs.getBigDecimal("unit_price"))
                    .quantity(rs.getInt("quantity"))
                    .build();
        }
    }
}
