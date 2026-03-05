package com.miracle.smart_ecommerce_security.domain.cart.service;

import com.miracle.smart_ecommerce_security.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_security.domain.cart.dto.CartResponse;
import com.miracle.smart_ecommerce_security.domain.cart.entity.CartItem;
import com.miracle.smart_ecommerce_security.domain.cart.entity.ShoppingCart;
import com.miracle.smart_ecommerce_security.domain.cart.repository.CartItemRepository;
import com.miracle.smart_ecommerce_security.domain.cart.repository.ShoppingCartRepository;
import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import com.miracle.smart_ecommerce_security.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.exception.BadRequestException;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_security.config.CacheConfig.*;

/**
 * Implementation of CartService using Spring Data JPA.
 *
 * Cache strategy:
 * - Cart is cached per user ID (key = userId)
 * - @CachePut re-caches the updated cart after every mutation so the next
 *   read is served from cache rather than hitting the DB
 * - @CacheEvict(allEntries) is NOT used here — only the specific user key
 *   is affected, so targeted eviction/put is always preferred
 * - clearCart evicts the key entirely since the cart is now empty;
 *   the next getCartByUserId will re-create/re-cache automatically
 * - getCartItemCount is not cached separately — it is derived from the
 *   cached cart on the service layer to avoid a separate cache key
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get all carts with pagination - admin use only.
     * Not cached — admin listing is dynamic and low-frequency.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CartResponse> getAllCarts(Pageable pageable) {
        log.debug("Getting all carts - pageable: {}", pageable);
        return cartRepository.findAll(pageable).map(this::buildCartResponse);
    }

    /**
     * Get cart by user ID.
     * Creates a new cart if one doesn't exist for the user.
     * Result cached per user ID.
     */
    @Override
    @Transactional
    @Cacheable(value = CART_CACHE, key = "#userId")
    public CartResponse getCartByUserId(UUID userId) {
        log.debug("Getting cart for user: {}", userId);
        ShoppingCart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    /**
     * Add an item to the user's cart.
     * Creates the cart if it doesn't exist.
     * Validates product availability and stock before adding.
     * If the product already exists in the cart, increments quantity.
     * Updated cart re-cached by userId after mutation.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = CART_CACHE, key = "#userId") },
            evict = { @CacheEvict(value = CART_CACHE, key = "#userId") }
    )
    public CartResponse addItemToCart(UUID userId, AddToCartRequest request) {
        log.info("Adding item to cart for user: {} - product: {}", userId, request.getProductId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available: " + product.getName());
        }

        if (!product.canBeOrdered(request.getQuantity())) {
            throw new BadRequestException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + product.getStockQuantity() +
                            ", Requested: " + request.getQuantity());
        }

        ShoppingCart cart = getOrCreateCart(userId);

        // If product already in cart, increment quantity instead of adding duplicate
        cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId())
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + request.getQuantity();
                            if (!product.canBeOrdered(newQty)) {
                                throw new BadRequestException(
                                        "Cannot add more of this product. Available: " + product.getStockQuantity());
                            }
                            cartItemRepository.updateQuantity(existing.getId(), newQty);
                            log.info("Incremented quantity for existing cart item: {}", existing.getId());
                        },
                        () -> {
                            try {
                                CartItem item = CartItem.builder()
                                        .cart(ShoppingCart.builder().id(cart.getId()).build())
                                        .product(Product.builder().id(request.getProductId()).build())
                                        .quantity(request.getQuantity())
                                        .build();
                                cartItemRepository.save(item);
                                log.info("New cart item added for product: {}", request.getProductId());
                            } catch (DataIntegrityViolationException e) {
                                log.error("Data integrity violation adding item to cart", e);
                                throw new DataIntegrityViolationException("Failed to add item to cart: " + e.getMessage());
                            }
                        }
                );

        return buildCartResponse(cart);
    }

    /**
     * Update the quantity of an item in the cart.
     * Validates that the item belongs to the user's cart.
     * Validates stock availability for the new quantity.
     * Updated cart re-cached by userId after mutation.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = CART_CACHE, key = "#userId") },
            evict = { @CacheEvict(value = CART_CACHE, key = "#userId") }
    )
    public CartResponse updateItemQuantity(UUID userId, UUID itemId, int quantity) {
        log.info("Updating item quantity: {} to {} for user: {}", itemId, quantity, userId);

        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId.toString()));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("CartItem", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to user's cart");
        }

        Product product = productRepository.findById(item.getProduct().getId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", item.getProduct().getId()));

        if (!product.canBeOrdered(quantity)) {
            throw new BadRequestException(
                    "Insufficient stock. Available: " + product.getStockQuantity() +
                            ", Requested: " + quantity);
        }

        cartItemRepository.updateQuantity(itemId, quantity);
        log.info("Item quantity updated successfully: {}", itemId);

        return buildCartResponse(cart);
    }

    /**
     * Remove an item from the cart.
     * Validates that the item belongs to the user's cart.
     * Updated cart re-cached by userId after removal.
     */
    @Override
    @Transactional
    @Caching(
            put  = { @CachePut(value = CART_CACHE, key = "#userId") },
            evict = { @CacheEvict(value = CART_CACHE, key = "#userId") }
    )
    public CartResponse removeItemFromCart(UUID userId, UUID itemId) {
        log.info("Removing item: {} from cart for user: {}", itemId, userId);

        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId.toString()));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("CartItem", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to user's cart");
        }

        cartItemRepository.deleteById(itemId);
        log.info("Item removed from cart successfully: {}", itemId);

        return buildCartResponse(cart);
    }

    /**
     * Clear all items from a user's cart.
     * Evicts the user's cart cache entry entirely.
     * Silently does nothing if the cart doesn't exist.
     */
    @Override
    @Transactional
    @CacheEvict(value = CART_CACHE, key = "#userId")
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);

        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            log.info("Cart cleared successfully for user: {}", userId);
        });
    }

    /**
     * Get total item count in a user's cart.
     * Returns 0 if no cart exists.
     * Not cached separately — count is derived on demand and is low-cost.
     */
    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> (int) cartItemRepository.countByCartId(cart.getId()))
                .orElse(0);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Finds the cart for a user or creates a new one if it doesn't exist.
     * Validates user existence before creating a new cart.
     */
    private ShoppingCart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            if (!userRepository.existsById(userId)) {
                throw ResourceNotFoundException.forResource("User", userId);
            }
            ShoppingCart newCart = ShoppingCart.builder()
                    .user(User.builder().id(userId).build())
                    .build();
            ShoppingCart saved = cartRepository.save(newCart);
            log.info("Created new cart for user: {}", userId);
            return saved;
        });
    }

    /**
     * Builds a full cart response from a ShoppingCart entity.
     * Batch-fetches all products for cart items to avoid N+1 queries.
     */
    private CartResponse buildCartResponse(ShoppingCart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        // Batch fetch all products to avoid N+1
        List<UUID> productIds = items.stream().map(i -> i.getProduct().getId()).toList();
        Map<UUID, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(item -> mapToCartItemResponse(item, productMap.get(item.getProduct().getId())))
                .toList();

        int totalItems = itemResponses.stream()
                .mapToInt(CartResponse.CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalValue = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .totalItems(totalItems)
                .totalValue(totalValue)
                .createdAt(cart.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Maps a CartItem to its response DTO using a pre-fetched product.
     */
    private CartResponse.CartItemResponse mapToCartItemResponse(CartItem item, Product product) {
        String productName = product != null ? product.getName() : "Unknown Product";
        String productImage = product != null ? product.getPrimaryImage() : null;
        BigDecimal unitPrice = product != null ? product.getPrice() : BigDecimal.ZERO;
        boolean inStock = product != null && product.isInStock();
        int availableStock = product != null ? product.getStockQuantity() : 0;

        return CartResponse.CartItemResponse.builder()
                .id(item.getId())
                .productName(productName)
                .productImage(productImage)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .inStock(inStock)
                .availableStock(availableStock)
                .build();
    }
}

