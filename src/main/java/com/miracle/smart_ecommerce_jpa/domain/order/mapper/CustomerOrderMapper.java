package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for CustomerOrder domain model.
 */
@Component
public class CustomerOrderMapper implements RowMapper<CustomerOrder> {

    @Override
    public CustomerOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
        return CustomerOrder.builder()
                .id(JdbcUtils.getUUID(rs, "id"))
                .userId(JdbcUtils.getUUID(rs, "user_id"))
                .orderNumber(rs.getString("order_number"))
                .status(rs.getString("status"))
                .paymentMethodId(JdbcUtils.getUUID(rs, "payment_method_id"))
                .shippingMethodId(JdbcUtils.getUUID(rs, "shipping_method_id"))
                .paymentStatus(rs.getString("payment_status"))
                .subtotal(rs.getBigDecimal("subtotal"))
                .total(rs.getBigDecimal("total_amount"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(JdbcUtils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}