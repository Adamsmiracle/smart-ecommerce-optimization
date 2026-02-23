package com.miracle.smart_ecommerce_jpa.domain.cart.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.CartItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for CartItem domain model.
 */
@Component
public class CartItemMapper implements RowMapper<CartItem> {

    @Override
    public CartItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return CartItem.builder()
                .id(Utils.getUUID(rs, "id"))
                .cartId(Utils.getUUID(rs, "cart_id"))
                .productId(Utils.getUUID(rs, "product_id"))
                .quantity(Utils.getInteger(rs, "quantity"))
                .build();
    }
}
