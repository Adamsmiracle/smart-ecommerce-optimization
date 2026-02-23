package com.miracle.smart_ecommerce_jpa.domain.category.service;

import com.miracle.smart_ecommerce_jpa.domain.category.entity.Category;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CreateCategoryRequest;
import com.miracle.smart_ecommerce_jpa.domain.category.dto.CategoryResponse;
import com.miracle.smart_ecommerce_jpa.exception.BadRequestException;
import com.miracle.smart_ecommerce_jpa.exception.DuplicateResourceException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.category.repository.CategoryRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of CategoryService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance
 * - Write operations use default REQUIRED propagation
 * - Dirty checking handles updates without explicit save()
 *
 * Cache strategy:
 * - Individual categories cached by ID
 * - Full category list cached separately
 * - All entries evicted on create, update, and delete
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - DuplicateResourceException for duplicate category names
 * - BadRequestException when deleting a category with associated products
 * - DataIntegrityViolationException caught as safety net for DB constraint violations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new category.
     * Checks for duplicate name before saving.
     * Result cached by ID after creation. All list caches evicted.
     */
    @Override
    @Transactional
    @Caching(
            put = { @CachePut(value = CATEGORIES_CACHE, key = "'id:' + #result.id") },
            evict = { @CacheEvict(value = CATEGORIES_CACHE, allEntries = true) }
    )
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getCategoryName());

        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category", "name", request.getCategoryName());
        }

        try {
            Category category = Category.builder()
                    .categoryName(request.getCategoryName())
                    .build();

            Category saved = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", saved.getId());
            return mapToResponse(saved);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating category: {}", request.getCategoryName(), e);
            throw new DataIntegrityViolationException("Failed to create category: " + e.getMessage());
        }
    }

    /**
     * Get category by ID.
     * Result cached by ID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CATEGORIES_CACHE, key = "'id:' + #id")
    public CategoryResponse getCategoryById(UUID id) {
        log.debug("Getting category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", id));
        return mapToResponse(category);
    }

    /**
     * Get all categories with product counts.
     * Uses a single JOIN query to fetch categories and their product counts
     * in one DB round-trip, avoiding N+1 queries.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Getting all categories");
        return categoryRepository.findAllWithProductCount().stream()
                .map(row -> {
                    Category category = (Category) row[0];
                    long productCount = (Long) row[1];
                    return CategoryResponse.builder()
                            .id(category.getId())
                            .categoryName(category.getCategoryName())
                            .productCount(productCount)
                            .build();
                })
                .toList();
    }

    /**
     * Update an existing category.
     * Checks for duplicate name if name is being changed.
     * Uses JPA dirty checking — no explicit save() needed.
     * All cache entries evicted after update.
     */
    @Override
    @Transactional
    @CacheEvict(value = CATEGORIES_CACHE, allEntries = true)
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", id));

        if (!category.getCategoryName().equals(request.getCategoryName())
                && categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category", "name", request.getCategoryName());
        }

        category.setCategoryName(request.getCategoryName());
        log.info("Category updated successfully: {}", id);
        return mapToResponse(category);
    }

    /**
     * Delete a category.
     * Prevents deletion if the category has associated products.
     * All cache entries evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = CATEGORIES_CACHE, allEntries = true)
    public void deleteCategory(UUID id) {
        log.info("Deleting category with ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Category", id);
        }

        if (productRepository.countByCategoryId(id) > 0) {
            throw new BadRequestException("Cannot delete category with associated products");
        }

        try {
            categoryRepository.deleteById(id);
            log.info("Category deleted successfully: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting category: {}", id, e);
            throw new DataIntegrityViolationException("Cannot delete category due to existing references: " + e.getMessage());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .productCount(productRepository.countByCategoryId(category.getId()))
                .build();
    }
}