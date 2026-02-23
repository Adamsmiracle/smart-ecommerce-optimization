package com.miracle.smart_ecommerce_jpa.domain.cart.repository;

import com.miracle.smart_ecommerce_jpa.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for CartItem domain model.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Find all items in a cart
     */
    List<CartItem> findByCartId(UUID cartId);

    /**
     * Find a cart item by cart ID and product ID
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Delete all items from a cart
     */
    void deleteByCartId(UUID cartId);

    /**
     * Count items in a cart
     */
    long countByCartId(UUID cartId);

    /**
     * Check if a product exists in a cart
     */
    boolean existsByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Update quantity of a cart item
     */
    @Modifying
    @Query("UPDATE CartItem c SET c.quantity = :quantity WHERE c.id = :id")
    void updateQuantity(@Param("id") UUID id, @Param("quantity") int quantity);
}