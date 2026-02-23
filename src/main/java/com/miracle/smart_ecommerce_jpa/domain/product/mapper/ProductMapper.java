package com.miracle.smart_ecommerce_jpa.domain.product.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for Product domain model.
 * Maps ResultSet rows to Product objects.
 */
@Component
public class ProductMapper implements RowMapper<Product> {

    @Override
    public Product mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return Product.builder()
                .id(Utils.getUUID(rs, "id"))
                .categoryId(Utils.getUUID(rs, "category_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price"))
                .stockQuantity(Utils.getInteger(rs, "stock_quantity"))
                .isActive(Utils.getBoolean(rs, "is_active"))
                .images(Utils.getStringListFromJsonb(rs, "images"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(Utils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}