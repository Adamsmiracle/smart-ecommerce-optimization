package com.miracle.smart_ecommerce_jpa.domain.review.mapper;

import com.miracle.smart_ecommerce_jpa.common.util.Utils;
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
                .id(Utils.getUUID(rs, "id"))
                .userId(Utils.getUUID(rs, "user_id"))
                .productId(Utils.getUUID(rs, "product_id"))
                .rating(Utils.getInteger(rs, "rating"))
                .comment(rs.getString("comment"))
                .createdAt(Utils.getOffsetDateTime(rs, "created_at"))
                .updatedAt(Utils.getOffsetDateTime(rs, "updated_at"))
                .build();
    }
}