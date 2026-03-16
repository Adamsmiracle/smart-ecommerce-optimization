package com.miracle.smart_ecommerce_security.domain.review.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.review.dto.CreateReviewRequest;
import com.miracle.smart_ecommerce_security.domain.review.dto.ReviewResponse;
import com.miracle.smart_ecommerce_security.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Product Review management.
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Product review management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Create a review", description = "Creates a new product review")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or user already reviewed this product"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or user not found")
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(review, "Review submitted successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all reviews", description = "Retrieve all reviews (paginated)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllReviews(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getAllReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID", description = "Retrieves a review by its unique ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(
            @Parameter(description = "Review ID") @PathVariable UUID id) {
        ReviewResponse review = reviewService.getReviewById(id);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product", description = "Retrieves all reviews for a specific product")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByProductId(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reviews by user", description = "Retrieves all reviews submitted by a specific user")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByUserId(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/product/{productId}/average-rating")
    @Operation(summary = "Get average rating", description = "Returns the average rating for a product")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(
            @Parameter(description = "Product ID") @PathVariable UUID productId) {
        Double averageRating = reviewService.getAverageRatingForProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(averageRating));
    }

    @GetMapping("/product/{productId}/count")
    @Operation(summary = "Get review count", description = "Returns the number of reviews for a product")
    public ResponseEntity<ApiResponse<Long>> getReviewCount(
            @Parameter(description = "Product ID") @PathVariable UUID productId) {
        long count = reviewService.countReviewsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/check")
    @Operation(summary = "Check if user reviewed product", description = "Checks if a user has already reviewed a product")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedProduct(
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Product ID") @RequestParam UUID productId) {
        boolean hasReviewed = reviewService.hasUserReviewedProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(hasReviewed));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review", description = "Updates an existing review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @Parameter(description = "Review ID") @PathVariable UUID id,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.updateReview(id, request);
        return ResponseEntity.ok(ApiResponse.success(review, "Review updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Deletes a review by ID")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "Review ID") @PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }
}
