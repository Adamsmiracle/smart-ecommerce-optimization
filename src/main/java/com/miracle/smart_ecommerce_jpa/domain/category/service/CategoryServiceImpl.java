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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of CategoryService using raw JDBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getCategoryName());

        // Check if category name already exists
        if (categoryRepository.existsByName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category", "name", request.getCategoryName());
        }

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        CategoryResponse response = mapToResponse(savedCategory);

        // Update cache with new category
        Cache byIdCache = cacheManager.getCache(CATEGORIES_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + savedCategory.getId(), response);
        }

        // Clear list cache
        evictCache();

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CATEGORIES_CACHE, key = "'id:' + #id")
    public CategoryResponse getCategoryById(UUID id) {
        log.debug("Getting category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", id));
        return mapToResponseWithDetails(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Getting all categories");
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        log.info("Updating category with ID: {}", id);

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Category", id));

        // Check if name is being changed to an existing one
        if (!existingCategory.getCategoryName().equals(request.getCategoryName())
                && categoryRepository.existsByName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category", "name", request.getCategoryName());
        }

        // Apply the requested change
        existingCategory.setCategoryName(request.getCategoryName());

        Category updatedCategory = categoryRepository.update(existingCategory);
        log.info("Category updated successfully: {}", id);

        // build response directly
        CategoryResponse response = mapToResponse(updatedCategory);

        // Update id cache
        Cache byIdCache = cacheManager.getCache(CATEGORIES_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        // Clear list cache
        evictCache();

        return response;
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        log.info("Deleting category with ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Category", id);
        }

        // Check if category has products
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BadRequestException("Cannot delete category with associated products");
        }


        categoryRepository.deleteById(id);
        log.info("Category deleted successfully: {}", id);

        // Evict from id cache
        Cache byIdCache = cacheManager.getCache(CATEGORIES_CACHE);
        if (byIdCache != null) {
            byIdCache.evict("id:" + id);
        }

        // Clear list cache
        evictCache();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .productCount(productRepository.countByCategoryId(category.getId()))
                .build();
    }

    private CategoryResponse mapToResponseWithDetails(Category category) {
        return mapToResponse(category);
    }

    /**
     * Helper method to evict all entries from a cache
     */
    private void evictCache() {
        Cache cache = cacheManager.getCache(CATEGORIES_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}
