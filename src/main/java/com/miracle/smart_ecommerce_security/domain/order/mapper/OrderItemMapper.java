package com.miracle.smart_ecommerce_security.domain.order.mapper;

import com.miracle.smart_ecommerce_security.common.util.Utils;
import com.miracle.smart_ecommerce_security.domain.order.entity.OrderItem;
import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
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

        Integer qty = Utils.getInteger(rs, "quantity");

        return OrderItem.builder()
                .id(Utils.getUUID(rs, "id"))
                .product(Product.builder().id(Utils.getUUID(rs, "product_id")).build())
                .unitPrice(unitPrice)
                .quantity(qty)
                .build();
        // Note: order relationship should be loaded separately via JPA
    }
}
