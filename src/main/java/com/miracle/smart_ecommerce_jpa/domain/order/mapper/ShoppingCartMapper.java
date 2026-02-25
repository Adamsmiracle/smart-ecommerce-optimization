package com.miracle.smart_ecommerce_jpa.domain.order.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
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
                .id(Utils.getUUID(rs, "id"))
                .user(User.builder().id(Utils.getUUID(rs, "user_id")).build())
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}