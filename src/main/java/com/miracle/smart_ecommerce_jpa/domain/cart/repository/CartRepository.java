package com.miracle.smart_ecommerce_jpa.domain.cart.repository;

import com.miracle.smart_ecommerce_jpa.domain.cart.entity.CartItem;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ShoppingCart and CartItem domain models.
 * Defines data access operations for shopping carts.
 */
public interface CartRepository {

    // ========================================================================
    // Shopping Cart Operations
    // ========================================================================

    /**
     * Save a new cart
     */
    ShoppingCart saveCart(ShoppingCart cart);

    /**
     * Find cart by ID
     */
    Optional<ShoppingCart> findCartById(UUID id);

    /**
     * Find cart by user ID
     */
    Optional<ShoppingCart> findCartByUserId(UUID userId);

    /**
     * Delete cart by ID
     */
    void deleteCartById(UUID id);

    /**
     * Delete cart by user ID
     */
    void deleteCartByUserId(UUID userId);

    /**
     * Check if cart exists for user
     */
    boolean existsCartByUserId(UUID userId);

    /**
     * Find all carts with pagination
     */
    List<ShoppingCart> findAll(int page, int size);

    /**
     * Count total carts
     */
    long count();

    // ========================================================================
    // Cart Item Operations
    // ========================================================================

    /**
     * Add item to cart
     */
    CartItem addItem(CartItem item);

    /**
     * Update cart item quantity
     */
    CartItem updateItemQuantity(UUID itemId, int quantity);

    /**
     * Find cart item by ID
     */
    Optional<CartItem> findItemById(UUID itemId);

    /**
     * Find cart items by cart ID
     */
    List<CartItem> findItemsByCartId(UUID cartId);

    /**
     * Find cart item by cart ID and product ID
     */
    Optional<CartItem> findItemByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Delete cart item by ID
     */
    void deleteItemById(UUID itemId);

    /**
     * Delete all items from cart
     */
    void deleteAllItemsByCartId(UUID cartId);

    /**
     * Count items in cart
     */
    int countItemsByCartId(UUID cartId);

    /**
     * Check if product exists in cart
     */
    boolean existsItemByCartIdAndProductId(UUID cartId, UUID productId);
}

