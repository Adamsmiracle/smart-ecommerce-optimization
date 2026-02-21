package com.miracle.smart_ecommerce_jpa.domain.product.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_jpa.domain.category.repository.CategoryRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of ProductService using raw JDBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CacheManager cacheManager;


    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());

        // Validate category exists
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw ResourceNotFoundException.forResource("Category", request.getCategoryId());
        }

        Product product = Product.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .images(request.getImages())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        ProductResponse response = mapToResponse(savedProduct);

        // Update caches with new product
        Cache byIdCache = cacheManager.getCache(PRODUCTS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + savedProduct.getId(), response);
        }

        // Clear list/search caches
        evictCache();
        evictCache();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_CACHE, key = "'id:' + #id")
    public ProductResponse getProductById(UUID id) {
        log.debug("Getting product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));
        return mapToResponseWithCategory(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        throw new UnsupportedOperationException("SKU lookup is not supported in current schema");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(int page, int size) {
        log.debug("Getting all products - page: {}, size: {}", page, size);
        List<Product> products = productRepository.findAll(page, size);
        long total = productRepository.count();

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getActiveProducts(int page, int size) {
        log.debug("Getting active products - page: {}, size: {}", page, size);
        List<Product> products = productRepository.findActiveProducts(page, size);
        long total = productRepository.countActive();

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(UUID categoryId, int page, int size) {
        log.debug("Getting products by category: {} - page: {}, size: {}", categoryId, page, size);

        if (!categoryRepository.existsById(categoryId)) {
            throw ResourceNotFoundException.forResource("Category", categoryId);
        }

        List<Product> products = productRepository.findByCategoryId(categoryId, page, size);
        long total = productRepository.countByCategoryId(categoryId);

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        log.debug("Searching products with keyword: {} - page: {}, size: {}", keyword, page, size);
        List<Product> products = productRepository.search(keyword, page, size);
        long total = productRepository.countActive();

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        log.debug("Getting products by price range: {} - {} - page: {}, size: {}", minPrice, maxPrice, page, size);
        List<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, page, size);
        long total = productRepository.countActive();

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsInStock(int page, int size) {
        log.debug("Getting products in stock - page: {}, size: {}", page, size);
        List<Product> products = productRepository.findInStock(page, size);
        long total = productRepository.countActive();

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, CreateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));

        // If categoryId provided, validate and set
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw ResourceNotFoundException.forResource("Category", request.getCategoryId());
            }
            existingProduct.setCategoryId(request.getCategoryId());
        }

        // Only update fields if they are provided in the request (support partial updates)
        if (request.getName() != null) existingProduct.setName(request.getName());
        if (request.getDescription() != null) existingProduct.setDescription(request.getDescription());
        if (request.getPrice() != null) existingProduct.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) existingProduct.setStockQuantity(request.getStockQuantity());
        if (request.getIsActive() != null) existingProduct.setIsActive(request.getIsActive());
        if (request.getImages() != null) existingProduct.setImages(request.getImages());

        Product updatedProduct = productRepository.update(existingProduct);
        log.info("Product updated successfully: {}", id);

        ProductResponse response = mapToResponse(updatedProduct);

        // Update id cache
        Cache byIdCache = cacheManager.getCache(PRODUCTS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        // Clear list/search caches
        evictCache();
        evictCache();

        return response;
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest request) {
        // Reuse the CreateProductRequest-based method logic but operate on UpdateProductRequest directly
        log.info("Updating product (partial) with ID: {}", id);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));

        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw ResourceNotFoundException.forResource("Category", request.getCategoryId());
            }
            existingProduct.setCategoryId(request.getCategoryId());
        }

        if (request.getName() != null) existingProduct.setName(request.getName());
        if (request.getDescription() != null) existingProduct.setDescription(request.getDescription());
        if (request.getPrice() != null) existingProduct.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) existingProduct.setStockQuantity(request.getStockQuantity());
        if (request.getIsActive() != null) existingProduct.setIsActive(request.getIsActive());
        if (request.getImages() != null) existingProduct.setImages(request.getImages());

        Product updatedProduct = productRepository.update(existingProduct);
        log.info("Product (partial) updated successfully: {}", id);

        ProductResponse response = mapToResponse(updatedProduct);

        Cache byIdCache = cacheManager.getCache(PRODUCTS_CACHE);
        if (byIdCache != null) {
            byIdCache.put("id:" + id, response);
        }

        evictCache();
        evictCache();

        return response;
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        log.info("Deleting product with ID: {}", id);

        // ensure product exists
        productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", id));

        productRepository.deleteById(id);
        log.info("Product deleted successfully: {}", id);

        // Evict from id cache
        Cache byIdCache = cacheManager.getCache(PRODUCTS_CACHE);
        if (byIdCache != null) {
            byIdCache.evict("id:" + id);
        }

        // Clear list/search caches
        evictCache();
    }

    @Override
    @Transactional
    public void activateProduct(UUID id) {
        log.info("Activating product with ID: {}", id);
        productRepository.setActiveStatus(id, true);

        // Evict caches - product status changed
        evictProductCaches(id);
    }

    @Override
    @Transactional
    public void deactivateProduct(UUID id) {
        log.info("Deactivating product with ID: {}", id);
        productRepository.setActiveStatus(id, false);

        // Evict caches - product status changed
        evictProductCaches(id);
    }

    @Override
    @Transactional
    public void updateStock(UUID id, int quantity) {
        log.info("Updating stock for product {} by {}", id, quantity);
        productRepository.updateStock(id, quantity);

        // Evict caches - stock changed
        evictProductCaches(id);
    }

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

    private ProductResponse mapToResponseWithCategory(Product product) {
        // If needed later, populate category details here
        return mapToResponse(product);
    }

    /**
     * Evict the configured products cache (single cache used for products)
     */
    private void evictCache() {
        Cache cache = cacheManager.getCache(PRODUCTS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Helper method to evict product from all caches
     */
    private void evictProductCaches(UUID productId) {
        Cache byIdCache = cacheManager.getCache(PRODUCTS_CACHE);
        if (byIdCache != null) {
            byIdCache.evict("id:" + productId);
        }

        // Get product to evict SKU cache
        productRepository.findById(productId).ifPresent(product -> {
            // Clear list/search caches
            evictCache();
        });
    }
}

