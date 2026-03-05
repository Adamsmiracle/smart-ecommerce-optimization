package com.miracle.smart_ecommerce_security.domain.order.mapper;

import com.miracle.smart_ecommerce_security.common.util.Utils;
import com.miracle.smart_ecommerce_security.domain.order.entity.PaymentMethod;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
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
                .id(Utils.getUUID(rs, "id"))
                .user(User.builder().id(Utils.getUUID(rs, "user_id")).build())
                .paymentType(rs.getString("payment_type"))
                .provider(rs.getString("provider"))
                .accountNumber(rs.getString("account_number"))
                .expiryDate(Utils.getOffsetDateTime(rs, "expiry_date"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}