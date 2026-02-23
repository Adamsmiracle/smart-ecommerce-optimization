package com.miracle.smart_ecommerce_jpa.domain.product.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.category.repository.CategoryRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of ProductService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance optimization
 * - Write operations use default REQUIRED propagation
 * - Dirty checking handles updates without explicit save()
 * - @Modifying queries (updateStock, setActiveStatus) run within the same transaction
 *
 * Cache strategy:
 * - Individual products cached by ID
 * - All product cache entries evicted on create, update, delete, activate,
 *   deactivate, and stock update to prevent stale data in listings
 *
 * Exception strategy:
 * - ResourceNotFoundException for missing entities
 * - DataIntegrityViolationException caught as safety net for DB constraint violations
 * - IllegalArgumentException for invalid input (e.g. negative stock)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Create a new product.
     * Validates category existence before saving.
     * Result cached by ID after creation.
     * All list caches evicted to reflect new product in listings.
     */
    @Override
    @Transactional
    @Caching(
            put = { @CachePut(value = PRODUCTS_CACHE, key = "'id:' + #result.id") },
            evict = { @CacheEvict(value = PRODUCTS_CACHE, allEntries = true) }
    )
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw ResourceNotFoundException.forResource("Category", request.getCategoryId());
        }

        try {
            Product product = Product.builder()
                    .categoryId(request.getCategoryId())
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .images(request.getImages())
                    .build();

            Product saved = productRepository.save(product);
            log.info("Product created successfully with ID: {}", saved.getId());
            return mapToResponse(saved);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating product: {}", request.getName(), e);
            throw new DataIntegrityViolationException("Failed to create product due to a data integrity constraint: " + e.getMessage());
        }
    }

    /**
     * Get product by ID.
     * Result cached by ID to avoid repeated DB lookups.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_CACHE, key = "'id:' + #id")
    public ProductResponse getProductById(UUID id) {
        log.debug("Getting product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));
        return mapToResponse(product);
    }

    /**
     * Get all products with pagination and sorting.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Getting all products - pageable: {}", pageable);

        Page<Product> productPage = productRepository.findAll(pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Get active products with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getActiveProducts(Pageable pageable) {
        log.debug("Getting active products - pageable: {}", pageable);

        Page<Product> productPage = productRepository.findActiveProducts(pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Get products by category with pagination.
     * Validates category existence before querying.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
        log.debug("Getting products by category: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw ResourceNotFoundException.forResource("Category", categoryId);
        }

        Page<Product> productPage = productRepository.findActiveByCategoryId(categoryId, pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Search products by keyword across name and description with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        log.debug("Searching products with keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be null or empty");
        }

        Page<Product> productPage = productRepository.search(keyword, pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Get products within a price range with pagination.
     * Validates that minPrice is not greater than maxPrice.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Getting products by price range: {} - {}", minPrice, maxPrice);

        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("Price range values cannot be null");
        }
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        Page<Product> productPage = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Get products in stock with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsInStock(Pageable pageable) {
        log.debug("Getting products in stock");

        Page<Product> productPage = productRepository.findInStock(pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), productPage.getTotalElements());
    }

    /**
     * Update an existing product.
     * Supports partial updates — only non-null fields are applied.
     * Uses JPA dirty checking — no explicit save() needed.
     * Cache evicted after update to prevent stale data.
     */
    @Override
    @Transactional
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));

        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw ResourceNotFoundException.forResource("Category", request.getCategoryId());
            }
            product.setCategoryId(request.getCategoryId());
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());
        if (request.getImages() != null) product.setImages(request.getImages());

        log.info("Product updated successfully: {}", id);
        return mapToResponse(product);
    }

    /**
     * Delete a product by ID.
     * Cache evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public void deleteProduct(UUID id) {
        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Product", id);
        }

        try {
            productRepository.deleteById(id);
            log.info("Product deleted successfully: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete product {} — it may be referenced by existing orders", id, e);
            throw new DataIntegrityViolationException("Cannot delete product as it is referenced by existing orders.");
        }
    }

    /**
     * Activate a product.
     * Uses existsById to avoid loading the full entity unnecessarily.
     */
    @Override
    @Transactional
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public void activateProduct(UUID id) {
        log.info("Activating product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Product", id);
        }
        productRepository.setActiveStatus(id, true);
        log.info("Product activated successfully: {}", id);
    }

    /**
     * Deactivate a product.
     * Uses existsById to avoid loading the full entity unnecessarily.
     */
    @Override
    @Transactional
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public void deactivateProduct(UUID id) {
        log.info("Deactivating product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Product", id);
        }
        productRepository.setActiveStatus(id, false);
        log.info("Product deactivated successfully: {}", id);
    }

    /**
     * Update stock quantity for a product.
     * Validates that quantity is not negative.
     */
    @Override
    @Transactional
    @CacheEvict(value = PRODUCTS_CACHE, allEntries = true)
    public void updateStock(UUID id, int quantity) {
        log.info("Updating stock for product: {} to quantity: {}", id, quantity);

        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        if (!productRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("Product", id);
        }

        productRepository.updateStock(id, quantity);
        log.info("Stock updated successfully for product: {}", id);
    }

    /**
     * Count total products.
     */
    @Override
    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .inStock(product.isInStock())
                .images(product.getImages())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}