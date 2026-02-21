package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.OrderItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for OrderItem domain model.
 * Maps ResultSet rows to OrderItem objects.
 */
@Component
public class OrderItemMapper implements RowMapper<OrderItem> {

    @Override
    public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        java.math.BigDecimal unitPrice = null;
        try {
            unitPrice = rs.getBigDecimal("unit_price");
        } catch (SQLException ignored) {
            // no-op
        }

        Integer qty = JdbcUtils.getInteger(rs, "quantity");

        return OrderItem.builder()
                .id(JdbcUtils.getUUID(rs, "id"))
                .orderId(JdbcUtils.getUUID(rs, "order_id"))
                .productId(JdbcUtils.getUUID(rs, "product_id"))
                .unitPrice(unitPrice)
                .quantity(qty)
                .build();
    }
}
