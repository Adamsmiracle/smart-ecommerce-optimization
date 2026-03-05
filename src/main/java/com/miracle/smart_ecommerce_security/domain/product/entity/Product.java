package com.miracle.smart_ecommerce_security.domain.product.entity;

import com.miracle.smart_ecommerce_security.domain.BaseModel;
import com.miracle.smart_ecommerce_security.domain.category.entity.Category;
import com.miracle.smart_ecommerce_security.domain.review.entity.ProductReview;
import com.miracle.smart_ecommerce_security.config.JsonbListStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product JPA entity - represents product table.
 */
@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseModel {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Price format is invalid")
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // JSONB field stored as List<String> for image URLs
    @Convert(converter = JsonbListStringConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    // Relationship to Category
    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Relationship to ProductReview
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductReview> reviews = new ArrayList<>();

    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    /**
     * Check if product can be ordered for given quantity
     */
    public boolean canBeOrdered(int quantity) {
        return isActive != null && isActive && isInStock() && stockQuantity >= quantity;
    }

    /**
     * Get primary image URL (first image or null)
     */
    public String getPrimaryImage() {
        return images != null && !images.isEmpty() ? images.get(0) : null;
    }

    /**
     * Calculate average rating from reviews
     */
    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }
        return reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Get review count
     */
    public int getReviewCount() {
        return reviews != null ? reviews.size() : 0;
    }
}