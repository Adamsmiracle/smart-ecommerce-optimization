package com.miracle.smart_ecommerce_jpa.domain.review.entity;

import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Product Review domain model (POJO) - represents product_review table.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductReview extends BaseModel {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;

}
