package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_jpa.domain.review.dto.ReviewResponse;
import com.miracle.smart_ecommerce_jpa.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
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
    public PageResponse<ReviewResponse> reviewsByProduct(@Argument UUID productId, @Argument int page, @Argument int size) {
        return reviewService.getReviewsByProductId(productId, page, size);
    }

    @QueryMapping
    public PageResponse<ReviewResponse> reviewsByUser(@Argument UUID userId, @Argument int page, @Argument int size) {
        return reviewService.getReviewsByUserId(userId, page, size);
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
    public ReviewResponse createReview(@Argument CreateReviewRequest input) {
        return reviewService.createReview(input);
    }

    @MutationMapping
    public ReviewResponse updateReview(@Argument UUID id, @Argument CreateReviewRequest input) {
        return reviewService.updateReview(id, input);
    }

    @MutationMapping
    public boolean deleteReview(@Argument UUID id) {
        reviewService.deleteReview(id);
        return true;
    }
}

