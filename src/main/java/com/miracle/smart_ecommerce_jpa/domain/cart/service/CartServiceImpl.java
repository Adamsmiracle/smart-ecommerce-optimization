package com.miracle.smart_ecommerce_jpa.domain.cart.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.CartItem;
import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.CartResponse;
import com.miracle.smart_ecommerce_jpa.exception.BadRequestException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.cart.repository.CartRepository;
import com.miracle.smart_ecommerce_jpa.domain.product.repository.ProductRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of CartService using raw JDBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional(readOnly = true)
    public PageResponse<CartResponse> getAllCarts(int page, int size) {
        log.debug("Getting all carts - page: {}, size: {}", page, size);

        List<ShoppingCart> carts = cartRepository.findAll(page, size);
        long total = cartRepository.count();

        List<CartResponse> responses = carts.stream()
                .map(this::buildCartResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    @Cacheable(value = CART_CACHE, key = "#userId")
    public CartResponse getCartByUserId(UUID userId) {
        log.debug("Getting cart for user: {}", userId);

        ShoppingCart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = CART_CACHE, key = "#userId")
    public CartResponse addItemToCart(UUID userId, AddToCartRequest request) {
        log.info("Adding item to cart for user: {} - product: {}", userId, request.getProductId());

        // Validate product exists and is in stock
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (!product.canBeOrdered(request.getQuantity())) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        // Get or create cart
        ShoppingCart cart = getOrCreateCart(userId);

        // Add item to cart
        CartItem item = CartItem.builder()
                .cartId(cart.getId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();

        cartRepository.addItem(item);
        log.info("Item added to cart successfully");

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = CART_CACHE, key = "#userId")
    public CartResponse updateItemQuantity(UUID userId, UUID itemId, int quantity) {
        log.info("Updating item quantity: {} to {} for user: {}", itemId, quantity, userId);

        ShoppingCart cart = cartRepository.findCartByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        CartItem item = cartRepository.findItemById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("CartItem", itemId));

        // Verify item belongs to user's cart
        if (!item.getCartId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to user's cart");
        }

        // Validate quantity
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        // Check stock
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", item.getProductId()));

        if (!product.canBeOrdered(quantity)) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        cartRepository.updateItemQuantity(itemId, quantity);
        log.info("Item quantity updated successfully");

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = CART_CACHE, key = "#userId")
    public CartResponse removeItemFromCart(UUID userId, UUID itemId) {
        log.info("Removing item from cart: {} for user: {}", itemId, userId);

        ShoppingCart cart = cartRepository.findCartByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        CartItem item = cartRepository.findItemById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("CartItem", itemId));

        // Verify item belongs to user's cart
        if (!item.getCartId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to user's cart");
        }

        cartRepository.deleteItemById(itemId);
        log.info("Item removed from cart successfully");

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = CART_CACHE, key = "#userId")
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);

        ShoppingCart cart = cartRepository.findCartByUserId(userId).orElse(null);
        if (cart != null) {
            cartRepository.deleteAllItemsByCartId(cart.getId());
        }
        log.info("Cart cleared successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(UUID userId) {
        ShoppingCart cart = cartRepository.findCartByUserId(userId).orElse(null);
        if (cart == null) {
            return 0;
        }
        return cartRepository.countItemsByCartId(cart.getId());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private ShoppingCart getOrCreateCart(UUID userId) {
        return cartRepository.findCartByUserId(userId)
                .orElseGet(() -> {
                    // Validate user exists
                    if (!userRepository.existsById(userId)) {
                        throw ResourceNotFoundException.forResource("User", userId);
                    }

                    ShoppingCart newCart = ShoppingCart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.saveCart(newCart);
                });
    }

    private CartResponse buildCartResponse(ShoppingCart cart) {
        List<CartItem> items = cartRepository.findItemsByCartId(cart.getId());

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        int totalItems = itemResponses.stream()
                .mapToInt(CartResponse.CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalValue = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .totalItems(totalItems)
                .totalValue(totalValue)
                .createdAt(OffsetDateTime.from(cart.getCreatedAt()))
                .items(itemResponses)
                .build();
    }

    private CartResponse.CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);

        String productName = product != null ? product.getName() : "Unknown Product";
        String productImage = product != null ? product.getPrimaryImage() : null;
        BigDecimal unitPrice = product != null ? product.getPrice() : BigDecimal.ZERO;
        boolean inStock = product != null && product.isInStock();
        int availableStock = product != null ? product.getStockQuantity() : 0;

        return CartResponse.CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
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

