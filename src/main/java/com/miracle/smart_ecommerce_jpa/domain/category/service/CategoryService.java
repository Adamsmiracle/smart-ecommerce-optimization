package com.miracle.smart_ecommerce_jpa.domain.category.service;

import com.miracle.smart_ecommerce_jpa.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Category operations.
 */
public interface CategoryService {

    /**
     * Create a new category
     */
    CategoryResponse createCategory(CreateCategoryRequest request);

    /**
     * Get category by ID
     */
    CategoryResponse getCategoryById(UUID id);

    /**
     * Get all categories
     */
    List<CategoryResponse> getAllCategories();


    /**
     * Update category
     */
    CategoryResponse updateCategory(UUID id, CreateCategoryRequest request);

    /**
     * Delete category
     */
    void deleteCategory(UUID id);

}

