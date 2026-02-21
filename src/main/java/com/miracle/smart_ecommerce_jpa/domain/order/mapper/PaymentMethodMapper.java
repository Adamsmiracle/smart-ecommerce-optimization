package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for PaymentMethod domain model.
 */
@Component
public class PaymentMethodMapper implements RowMapper<PaymentMethod> {

    @Override
    public PaymentMethod mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PaymentMethod.builder()
                .id(JdbcUtils.getUUID(rs, "id"))
                .userId(JdbcUtils.getUUID(rs, "user_id"))
                .paymentType(rs.getString("payment_type"))
                .provider(rs.getString("provider"))
                .accountNumber(rs.getString("account_number"))
                .expiryDate(JdbcUtils.getOffsetDateTime(rs, "expiry_date"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}