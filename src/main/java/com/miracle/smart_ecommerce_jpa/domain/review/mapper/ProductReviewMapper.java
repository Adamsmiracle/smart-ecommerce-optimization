package com.miracle.smart_ecommerce_jpa.domain.review.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for ProductReview domain model.
 * Maps ResultSet rows to ProductReview objects.
 */
@Component
public class ProductReviewMapper implements RowMapper<ProductReview> {

    @Override
    public ProductReview mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ProductReview.builder()
                .id(JdbcUtils.getUUID(rs, "id"))
                .userId(JdbcUtils.getUUID(rs, "user_id"))
                .productId(JdbcUtils.getUUID(rs, "product_id"))
                .rating(JdbcUtils.getInteger(rs, "rating"))
                .comment(rs.getString("comment"))
                .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(JdbcUtils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}