package com.miracle.smart_ecommerce_jpa.domain.product.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
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
                .id(JdbcUtils.getUUID(rs, "id"))
                .categoryId(JdbcUtils.getUUID(rs, "category_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price"))
                .stockQuantity(JdbcUtils.getInteger(rs, "stock_quantity"))
                .isActive(JdbcUtils.getBoolean(rs, "is_active"))
                .images(JdbcUtils.getStringListFromJsonb(rs, "images"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(JdbcUtils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}