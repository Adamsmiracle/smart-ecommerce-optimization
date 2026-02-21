package com.miracle.smart_ecommerce_jpa.domain.review.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.review.entity.ProductReview;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.ReviewResponse;
import com.miracle.smart_ecommerce_jpa.exception.DuplicateResourceException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.domain.review.repository.ReviewRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of ReviewService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;


    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        log.info("Creating review for product: {} by user: {}", request.getProductId(), request.getUserId());

        // Verify product exists
        if (!productRepository.existsById(request.getProductId())) {
            throw ResourceNotFoundException.forResource("Product", request.getProductId());
        }

        // Verify user exists
        if (!userRepository.existsById(request.getUserId())) {
            throw ResourceNotFoundException.forResource("User", request.getUserId());
        }

        // Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(request.getUserId(), request.getProductId())) {
            throw new DuplicateResourceException("Review", "user-product",
                    request.getUserId() + "-" + request.getProductId());
        }

        ProductReview review = ProductReview.builder()
                .productId(request.getProductId())
                .userId(request.getUserId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        ProductReview savedReview = reviewRepository.save(review);
        log.info("Review created successfully with ID: {}", savedReview.getId());

        ReviewResponse response = mapToResponse(savedReview);

        // Update cache with new review
        Cache cache = cacheManager.getCache(REVIEWS_CACHE);
        if (cache != null) {
            cache.put("id:" + savedReview.getId(), response);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = REVIEWS_CACHE, key = "'id:' + #id")
    public ReviewResponse getReviewById(UUID id) {
        log.debug("Getting review by ID: {}", id);
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", id));
        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProductId(UUID productId, int page, int size) {
        log.debug("Getting reviews for product: {} - page: {}, size: {}", productId, page, size);
        List<ProductReview> reviews = reviewRepository.findByProductId(productId, page, size);
        long total = reviewRepository.countByProductId(productId);

        List<ReviewResponse> responses = reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByUserId(UUID userId, int page, int size) {
        log.debug("Getting reviews by user: {} - page: {}, size: {}", userId, page, size);
        List<ProductReview> reviews = reviewRepository.findByUserId(userId, page, size);
        // Simplified count - ideally should count user's reviews
        long total = reviews.size();

        List<ReviewResponse> responses = reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviews(int page, int size) {
        log.debug("Getting all reviews - page: {}, size: {}", page, size);
        List<ProductReview> reviews = reviewRepository.findAll(page, size);
        long total = reviewRepository.countAll();

        List<ReviewResponse> responses = reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingForProduct(UUID productId) {
        log.debug("Getting average rating for product: {}", productId);
        return reviewRepository.getAverageRatingByProductId(productId);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID id, CreateReviewRequest request) {
        log.info("Updating review: {}", id);

        ProductReview existingReview = reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", id));

        existingReview.setRating(request.getRating());
        existingReview.setComment(request.getComment());

        ProductReview updatedReview = reviewRepository.update(existingReview);
        log.info("Review updated successfully: {}", id);

        ReviewResponse response = mapToResponse(updatedReview);

        // Update cache
        Cache cache = cacheManager.getCache(REVIEWS_CACHE);
        if (cache != null) {
            cache.put("id:" + id, response);
        }

        return response;
    }

    @Override
    @Transactional
    public void deleteReview(UUID id) {
        log.info("Deleting review: {}", id);
        if (!reviewRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Review", id);
        }
        reviewRepository.deleteById(id);
        log.info("Review deleted successfully: {}", id);

        // Evict from cache
        Cache cache = cacheManager.getCache(REVIEWS_CACHE);
        if (cache != null) {
            cache.evict("id:" + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(UUID userId, UUID productId) {
        return reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReviewsByProductId(UUID productId) {
        return reviewRepository.countByProductId(productId);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ReviewResponse mapToResponse(ProductReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

