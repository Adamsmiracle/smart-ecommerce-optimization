package com.miracle.smart_ecommerce_jpa.domain.review.repository;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_jpa.common.util.JdbcUtils;
import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of ReviewRepository.
 */
@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ProductReview> reviewRowMapper;

    public ReviewRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewRowMapper = new ProductReviewRowMapper();
    }

    @Override
    @Transactional
    public ProductReview save(ProductReview review) {
        String sql = """
                INSERT INTO product_review (product_id, user_id, rating, comment, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, product_id, user_id, rating, comment, created_at, updated_at
                """;

        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.queryForObject(sql, reviewRowMapper,
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getComment(),
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }

    @Override
    @Transactional
    public ProductReview update(ProductReview review) {
        String sql = """
                UPDATE product_review
                SET rating = ?, comment = ?, updated_at = ?
                WHERE id = ?
                RETURNING id, product_id, user_id, rating, comment, created_at, updated_at
                """;

        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.queryForObject(sql, reviewRowMapper,
                review.getRating(),
                review.getComment(),
                Timestamp.from(now.toInstant()),
                review.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductReview> findById(UUID id) {
        String sql = "SELECT * FROM product_review WHERE id = ?";
        try {
            ProductReview review = jdbcTemplate.queryForObject(sql, reviewRowMapper, id);
            return Optional.ofNullable(review);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> findByProductId(UUID productId, int page, int size) {
        String sql = "SELECT * FROM product_review WHERE product_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, reviewRowMapper, productId, size, page * size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> findByUserId(UUID userId, int page, int size) {
        String sql = "SELECT * FROM product_review WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, reviewRowMapper, userId, size, page * size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> findAll(int page, int size) {
        String sql = "SELECT * FROM product_review ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, reviewRowMapper, size, page * size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        String sql = "SELECT COUNT(*) FROM product_review";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingByProductId(UUID productId) {
        String sql = "SELECT COALESCE(AVG(rating), 0) FROM product_review WHERE product_id = ?";
        return jdbcTemplate.queryForObject(sql, Double.class, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByProductId(UUID productId) {
        String sql = "SELECT COUNT(*) FROM product_review WHERE product_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, productId);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM product_review WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndProductId(UUID userId, UUID productId) {
        String sql = "SELECT COUNT(*) FROM product_review WHERE user_id = ? AND product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, productId);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM product_review WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * RowMapper for ProductReview
     */
    private static class ProductReviewRowMapper implements RowMapper<ProductReview> {
        @Override
        public ProductReview mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ProductReview.builder()
                    .id(rs.getObject("id", UUID.class))
                    .productId(rs.getObject("product_id", UUID.class))
                    .userId(rs.getObject("user_id", UUID.class))
                    .rating(rs.getInt("rating"))
                    .comment(rs.getString("comment"))
                    .createdAt(JdbcUtils.getOffsetDateTime(rs, "created_at"))
                    .updatedAt(JdbcUtils.getOffsetDateTime(rs, "updated_at"))
                    .build();
        }
    }
}
