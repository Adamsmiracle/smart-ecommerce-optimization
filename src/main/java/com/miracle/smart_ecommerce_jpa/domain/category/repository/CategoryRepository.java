package com.miracle.smart_ecommerce_jpa.domain.category.repository;

import com.miracle.smart_ecommerce_jpa.domain.category.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Category domain model.
 * Defines data access operations for categories.
 */
public interface CategoryRepository {

    /**
     * Save a new category
     */
    Category save(Category category);

    /**
     * Update an existing category
     */
    Category update(Category category);

    /**
     * Find category by ID
     */
    Optional<Category> findById(UUID id);

    /**
     * Find category by name
     */
    Optional<Category> findByName(String name);

    /**
     * Find all categories
     */
    List<Category> findAll();

    /**
     * Find subcategories by parent ID
     */
    List<Category> findByParentId(UUID parentId);

    /**
     * Delete category by ID
     */
    void deleteById(UUID id);

    /**
     * Check if category exists by ID
     */
    boolean existsById(UUID id);

    /**
     * Check if category name exists
     */
    boolean existsByName(String name);

    /**
     * Count total categories
     */
    long count();

}

