package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
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
                .id(Utils.getUUID(rs, "id"))
                .userId(Utils.getUUID(rs, "user_id"))
                .orderNumber(rs.getString("order_number"))
                .status(rs.getString("status"))
                .paymentMethodId(Utils.getUUID(rs, "payment_method_id"))
                .shippingMethodId(Utils.getUUID(rs, "shipping_method_id"))
                .paymentStatus(rs.getString("payment_status"))
                .subtotal(rs.getBigDecimal("subtotal"))
                .total(rs.getBigDecimal("total_amount"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(Utils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}