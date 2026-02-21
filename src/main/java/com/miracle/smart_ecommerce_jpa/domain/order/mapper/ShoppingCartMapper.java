package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for ShoppingCart domain model.
 */
@Component
public class ShoppingCartMapper implements RowMapper<ShoppingCart> {

    @Override
    public ShoppingCart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ShoppingCart.builder()
                .id(JdbcUtils.getUUID(rs, "id"))
                .userId(JdbcUtils.getUUID(rs, "user_id"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}