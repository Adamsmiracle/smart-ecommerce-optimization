package com.miracle.smart_ecommerce_jpa.domain.review.repository;

import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product Review operations.
 */
public interface ReviewRepository {

    ProductReview save(ProductReview review);

    ProductReview update(ProductReview review);

    Optional<ProductReview> findById(UUID id);

    List<ProductReview> findByProductId(UUID productId, int page, int size);

    List<ProductReview> findByUserId(UUID userId, int page, int size);

    /**
     * Find all reviews (paginated)
     */
    List<ProductReview> findAll(int page, int size);

    /**
     * Count all reviews
     */
    long countAll();

    Double getAverageRatingByProductId(UUID productId);

    long countByProductId(UUID productId);

    boolean existsById(UUID id);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    void deleteById(UUID id);
}
