package com.miracle.smart_ecommerce_jpa.domain.review.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.ReviewResponse;
import com.miracle.smart_ecommerce_jpa.exception.DuplicateResourceException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.domain.review.repository.ReviewRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of ReviewService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance optimization
 * - Write operations use default REQUIRED propagation
 * - Dirty checking handles updates without explicit save()
 *
 * Cache strategy:
 * - Individual reviews cached by ID
 * - All review cache entries evicted on create, update, delete
 *   to avoid stale average ratings or product review lists
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - DuplicateResourceException for application-level duplicate checks
 * - DataIntegrityViolationException caught as safety net for concurrent duplicate inserts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Create a new review.
     * Validates product and user existence before saving.
     * Guards against duplicate reviews at both application and DB level.
     */
    @Override
    @Transactional
    @CacheEvict(value = REVIEWS_CACHE, allEntries = true)
    public ReviewResponse createReview(CreateReviewRequest request) {
        log.info("Creating review for product: {} by user: {}", request.getProductId(), request.getUserId());

        if (!productRepository.existsById(request.getProductId())) {
            throw ResourceNotFoundException.forResource("Product", request.getProductId());
        }

        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        if (reviewRepository.existsByUser_IdAndProduct_Id(request.getUserId(), request.getProductId())) {
            throw new DuplicateResourceException("Review", "user-product",
                    request.getUserId() + "-" + request.getProductId());
        }

        try {
            ProductReview review = ProductReview.builder()
                    .product(Product.builder().id(request.getProductId()).build())
                    .user(User.builder().id(request.getUserId()).build())
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();

            ProductReview saved = reviewRepository.save(review);
            log.info("Review created successfully with ID: {}", saved.getId());
            return mapToResponse(saved);

        } catch (DataIntegrityViolationException e) {
            // Safety net for concurrent duplicate review submissions
            log.warn("Duplicate review detected at DB level for user: {} and product: {}",
                    request.getUserId(), request.getProductId());
            throw new DuplicateResourceException("Review", "user-product",
                    request.getUserId() + "-" + request.getProductId());
        }
    }

    /**
     * Get review by ID.
     * Result cached by ID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = REVIEWS_CACHE, key = "'id:' + #id")
    public ReviewResponse getReviewById(UUID id) {
        log.debug("Getting review by ID: {}", id);
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", id));
        return mapToResponse(review);
    }

    /**
     * Get paginated reviews for a product.
     * Validates product exists before querying.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProductId(UUID productId, Pageable pageable) {
        log.debug("Getting reviews for product: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw ResourceNotFoundException.forResource("Product", productId);
        }

        Page<ProductReview> reviewPage = reviewRepository.findByProduct_Id(productId, pageable);
        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), reviewPage.getTotalElements());
    }

    /**
     * Get paginated reviews by a user.
     * Validates user exists before querying.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByUserId(UUID userId, Pageable pageable) {
        log.debug("Getting reviews by user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        Page<ProductReview> reviewPage = reviewRepository.findByUser_Id(userId, pageable);
        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), reviewPage.getTotalElements());
    }

    /**
     * Get all reviews with pagination - admin use.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviews(Pageable pageable) {
        log.debug("Getting all reviews");

        Page<ProductReview> reviewPage = reviewRepository.findAll(pageable);
        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), reviewPage.getTotalElements());
    }

    /**
     * Get average rating for a product.
     * Validates product exists before querying.
     * Returns null if product has no reviews.
     */
    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingForProduct(UUID productId) {
        log.debug("Getting average rating for product: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw ResourceNotFoundException.forResource("Product", productId);
        }

        return reviewRepository.getAverageRatingByProductId(productId);
    }

    /**
     * Update an existing review.
     * Only rating and comment can be changed.
     * Uses JPA dirty checking — no explicit save() needed.
     * All review cache entries evicted after update.
     */
    @Override
    @Transactional
    @CacheEvict(value = REVIEWS_CACHE, allEntries = true)
    public ReviewResponse updateReview(UUID id, CreateReviewRequest request) {
        log.info("Updating review: {}", id);

        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", id));

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        log.info("Review updated successfully: {}", id);
        return mapToResponse(review);
    }

    /**
     * Delete a review by ID.
     * All review cache entries evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = REVIEWS_CACHE, allEntries = true)
    public void deleteReview(UUID id) {
        log.info("Deleting review: {}", id);
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", id));
        reviewRepository.delete(review);
        log.info("Review deleted successfully: {}", id);
    }

    /**
     * Check if a user has already reviewed a product.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(UUID userId, UUID productId) {
        return reviewRepository.existsByUser_IdAndProduct_Id(userId, productId);
    }

    /**
     * Count total reviews for a product.
     */
    @Override
    @Transactional(readOnly = true)
    public long countReviewsByProductId(UUID productId) {
        return reviewRepository.countByProduct_Id(productId);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ReviewResponse mapToResponse(ProductReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}