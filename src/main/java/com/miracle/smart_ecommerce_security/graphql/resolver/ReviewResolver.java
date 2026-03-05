package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_security.domain.review.dto.ReviewResponse;
import com.miracle.smart_ecommerce_security.domain.review.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * GraphQL Resolver for Review entity.
 *
 * Access: ADMIN + CUSTOMER for queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
public class ReviewResolver {

    private final ReviewService reviewService;

    // =====================
    // QUERIES
    // =====================

    @QueryMapping
    public ReviewResponse review(@Argument UUID id) {
        return reviewService.getReviewById(id);
    }

    @QueryMapping
    public Page<ReviewResponse> reviewsByProduct(@Argument UUID productId, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsByProductId(productId, pageable);
    }

    @QueryMapping
    public Page<ReviewResponse> reviewsByUser(@Argument UUID userId, @Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsByUserId(userId, pageable);
    }

    @QueryMapping
    public Double productAverageRating(@Argument UUID productId) {
        return reviewService.getAverageRatingForProduct(productId);
    }

    @QueryMapping
    public Boolean hasUserReviewedProduct(@Argument UUID userId, @Argument UUID productId) {
        return reviewService.hasUserReviewedProduct(userId, productId);
    }

    // =====================
    // MUTATIONS
    // =====================

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ReviewResponse createReview(@Argument CreateReviewRequest input) {
        return reviewService.createReview(input);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ReviewResponse updateReview(@Argument UUID id, @Argument CreateReviewRequest input) {
        return reviewService.updateReview(id, input);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public boolean deleteReview(@Argument UUID id) {
        reviewService.deleteReview(id);
        return true;
    }
}
