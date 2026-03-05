package com.miracle.smart_ecommerce_security.domain.product.mapper;

import com.miracle.smart_ecommerce_security.common.util.Utils;
import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import com.miracle.smart_ecommerce_security.domain.category.entity.Category;
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
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(rs.getBigDecimal("price"))
                .stockQuantity(Utils.getInteger(rs, "stock_quantity"))
                .isActive(rs.getObject("is_active") == null ? Boolean.TRUE : rs.getBoolean("is_active"))
                .category(Category.builder().id(Utils.getUUID(rs, "category_id")).build())
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .build();
    }
}