package com.miracle.smart_ecommerce_jpa.domain.category.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Product Category domain model (POJO) - represents product_category table.
 * Supports hierarchical categories with parent-child relationships.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Category {

    @NotNull(message = "id is required")
    private UUID id;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String categoryName;

}

