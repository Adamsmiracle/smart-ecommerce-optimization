package com.miracle.smart_ecommerce_security.domain.product.repository;

import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Repository interface for Product domain model.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Find active products with pagination
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    Page<Product> findActiveProducts(Pageable pageable);

    /**
     * Find products by category ID with pagination (via relation path)
     */
    Page<Product> findByCategory_Id(UUID categoryId, Pageable pageable);

    /**
     * Find active products by category ID with pagination (via relation path)
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findActiveByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Search products by name or description with pagination
     */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find products within a price range with pagination
     */
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find products in stock with pagination
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 AND p.isActive = true")
    Page<Product> findInStock(Pageable pageable);

    /**
     * Count active products
     */
    long countByIsActiveTrue();

    /**
     * Count products by category (via relation path)
     */
    long countByCategory_Id(UUID categoryId);

    /**
     * Update product stock quantity
     */
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = :quantity WHERE p.id = :productId")
    void updateStock(@Param("productId") UUID productId, @Param("quantity") int quantity);

    /**
     * Set product active status
     */
    @Modifying
    @Query("UPDATE Product p SET p.isActive = :isActive WHERE p.id = :id")
    void setActiveStatus(@Param("id") UUID id, @Param("isActive") boolean isActive);

    /**
     * Multi-criteria search for Products (moved here from consolidated/complete repository)
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:stockQuantity IS NULL OR p.stockQuantity >= :stockQuantity) AND " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "p.isActive = true")
    Page<Product> findProductsByMultipleCriteria(@Param("categoryId") UUID categoryId,
                                                 @Param("minPrice") BigDecimal minPrice,
                                                 @Param("maxPrice") BigDecimal maxPrice,
                                                 @Param("stockQuantity") Integer stockQuantity,
                                                 @Param("name") String name,
                                                 Pageable pageable);
}