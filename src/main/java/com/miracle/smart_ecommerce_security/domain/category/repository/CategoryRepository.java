package com.miracle.smart_ecommerce_security.domain.category.repository;

import com.miracle.smart_ecommerce_security.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for Category domain model.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find category by name
     */
    Optional<Category> findByCategoryName(String categoryName);

    /**
     * Check if category name exists
     */
    boolean existsByCategoryName(String categoryName);

    /**
     * Fetch all categories with their product counts in a single query.
     * Returns Object[] where [0] is Category and [1] is Long product count.
     * Avoids N+1 queries when building category list responses.
     */
    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN Product p ON p.category = c GROUP BY c")
    List<Object[]> findAllWithProductCount();
}