package com.miracle.smart_ecommerce_jpa.domain.cart.repository;

import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for ShoppingCart domain model.
 */
@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {

    /**
     * Find cart by user ID
     */
    Optional<ShoppingCart> findByUserId(UUID userId);

    /**
     * Delete cart by user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Check if cart exists for user
     */
    boolean existsByUserId(UUID userId);
}