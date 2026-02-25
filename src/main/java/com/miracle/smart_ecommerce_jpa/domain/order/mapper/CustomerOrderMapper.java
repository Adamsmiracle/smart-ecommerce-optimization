package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.CustomerOrder;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
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
                .user(User.builder().id(Utils.getUUID(rs, "user_id")).build())
                .orderNumber(rs.getString("order_number"))
                .status(rs.getString("status"))
                .paymentMethod(rs.getObject("payment_method_id") == null ? null : PaymentMethod.builder().id(Utils.getUUID(rs, "payment_method_id")).build())
                .shippingMethod(rs.getObject("shipping_method_id") == null ? null : ShippingMethod.builder().id(Utils.getUUID(rs, "shipping_method_id")).build())
                .subtotal(rs.getBigDecimal("subtotal"))
                .total(rs.getBigDecimal("total"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(Utils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}