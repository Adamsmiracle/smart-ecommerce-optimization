package com.miracle.smart_ecommerce_security.domain.category.service;

import com.miracle.smart_ecommerce_security.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_security.domain.category.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Get all categories (paginated)
     */
    Page<CategoryResponse> getAllCategories(Pageable pageable);


    /**
     * Update category
     */
    CategoryResponse updateCategory(UUID id, CreateCategoryRequest request);

    /**
     * Delete category
     */
    void deleteCategory(UUID id);

}

