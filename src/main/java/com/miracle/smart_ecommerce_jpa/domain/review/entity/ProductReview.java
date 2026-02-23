package com.miracle.smart_ecommerce_jpa.domain.review.entity;

import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Product Review JPA entity - represents product_review table.
 */
@Entity
@Table(name = "product_review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductReview extends BaseModel {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    @Column(name = "comment", length = 2000)
    private String comment;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Product product;
}
