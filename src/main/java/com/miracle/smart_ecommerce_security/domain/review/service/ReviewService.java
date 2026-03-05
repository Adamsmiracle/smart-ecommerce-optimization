package com.miracle.smart_ecommerce_security.domain.review.service;

import com.miracle.smart_ecommerce_security.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_security.domain.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Product Review operations.
 */
public interface ReviewService {

    /**
     * Create a new review
     */
    ReviewResponse createReview(CreateReviewRequest request);

    /**
     * Get review by ID
     */
    ReviewResponse getReviewById(UUID id);

    /**
     * Get all reviews for a product
     */
    Page<ReviewResponse> getReviewsByProductId(UUID productId, Pageable pageable);

    /**
     * Get all reviews by a user
     */
    Page<ReviewResponse> getReviewsByUserId(UUID userId, Pageable pageable);

    /**
     * Get paginated list of all reviews
     */
    Page<ReviewResponse> getAllReviews(Pageable pageable);

    /**
     * Get average rating for a product
     */
    Double getAverageRatingForProduct(UUID productId);

    /**
     * Update a review
     */
    ReviewResponse updateReview(UUID id, CreateReviewRequest request);

    /**
     * Delete a review
     */
    void deleteReview(UUID id);

    /**
     * Check if user has reviewed a product
     */
    boolean hasUserReviewedProduct(UUID userId, UUID productId);

    /**
     * Count reviews for a product
     */
    long countReviewsByProductId(UUID productId);
}
