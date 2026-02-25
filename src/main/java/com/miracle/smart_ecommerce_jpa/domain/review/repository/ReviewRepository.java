package com.miracle.smart_ecommerce_jpa.domain.review.repository;

import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository interface for ProductReview domain model.
 */
@Repository
public interface ReviewRepository extends JpaRepository<ProductReview, UUID> {

    /**
     * Find all reviews for a product with pagination
     */
    Page<ProductReview> findByProduct_Id(UUID productId, Pageable pageable);

    /**
     * Find all reviews by a user with pagination
     */
    Page<ProductReview> findByUser_Id(UUID userId, Pageable pageable);

    /**
     * Get average rating for a product
     */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    /**
     * Count reviews for a product
     */
    long countByProduct_Id(UUID productId);

    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r WHERE r.product.id = :productId GROUP BY r.rating")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") UUID productId);

    /**
     * Check if a user has already reviewed a product
     */
    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);
}