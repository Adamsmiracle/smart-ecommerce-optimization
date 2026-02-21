package com.miracle.smart_ecommerce_jpa.domain.cart.repository;

import java.time.OffsetDateTime;

import com.miracle.smart_ecommerce_jpa.domain.cart.entity.CartItem;
import com.miracle.smart_ecommerce_jpa.domain.cart.entity.ShoppingCart;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.cart.mapper.CartItemMapper;
import com.miracle.smart_ecommerce_jpa.domain.order.mapper.ShoppingCartMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of CartRepository.
 */
@Repository
public class CartRepositoryImpl implements CartRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ShoppingCartMapper cartRowMapper;
    private final CartItemMapper itemRowMapper;

    public CartRepositoryImpl(JdbcTemplate jdbcTemplate,
                              ShoppingCartMapper cartRowMapper,
                              CartItemMapper itemRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.cartRowMapper = cartRowMapper;
        this.itemRowMapper = itemRowMapper;
    }

    // ========================================================================
    // Shopping Cart Operations
    // ========================================================================

    @Override
    @Transactional
    public ShoppingCart saveCart(ShoppingCart cart) {
        String sql = """
            INSERT INTO shopping_cart (user_id, created_at)
            VALUES (?, ?)
            RETURNING *
            """;

        return jdbcTemplate.queryForObject(sql, cartRowMapper,
                cart.getUserId(),
                Timestamp.from(OffsetDateTime.now().toInstant())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShoppingCart> findCartById(UUID id) {
        String sql = "SELECT * FROM shopping_cart WHERE id = ?";
        try {
            ShoppingCart cart = jdbcTemplate.queryForObject(sql, cartRowMapper, id);
            return Optional.ofNullable(cart);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShoppingCart> findCartByUserId(UUID userId) {
        String sql = "SELECT * FROM shopping_cart WHERE user_id = ?";
        try {
            ShoppingCart cart = jdbcTemplate.queryForObject(sql, cartRowMapper, userId);
            return Optional.ofNullable(cart);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteCartById(UUID id) {
        String sql = "DELETE FROM shopping_cart WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    @Transactional
    public void deleteCartByUserId(UUID userId) {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsCartByUserId(UUID userId) {
        String sql = "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingCart> findAll(int page, int size) {
        String sql = "SELECT * FROM shopping_cart ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, cartRowMapper, size, page * size);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        String sql = "SELECT COUNT(*) FROM shopping_cart";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    // ========================================================================
    // Cart Item Operations
    // ========================================================================

    @Override
    @Transactional
    public CartItem addItem(CartItem item) {
        String sql = """
            INSERT INTO cart_item (cart_id, product_id, quantity)
            VALUES (?, ?, ?)
            ON CONFLICT (cart_id, product_id)
            DO UPDATE SET quantity = cart_item.quantity + EXCLUDED.quantity
            RETURNING *
            """;

        return jdbcTemplate.queryForObject(sql, itemRowMapper,
                item.getCartId(),
                item.getProductId(),
                item.getQuantity()
         );

    }

    @Override
    @Transactional
    public CartItem updateItemQuantity(UUID itemId, int quantity) {
        String sql = """
            UPDATE cart_item SET quantity = ?
            WHERE id = ?
            RETURNING *
            """;

        try {
            return jdbcTemplate.queryForObject(sql, itemRowMapper,
                    quantity,
                    itemId
             );
        } catch (EmptyResultDataAccessException e) {
            throw ResourceNotFoundException.forResource("CartItem", itemId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartItem> findItemById(UUID itemId) {
        String sql = "SELECT * FROM cart_item WHERE id = ?";
        try {
            CartItem item = jdbcTemplate.queryForObject(sql, itemRowMapper, itemId);
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> findItemsByCartId(UUID cartId) {
        String sql = "SELECT * FROM cart_item WHERE cart_id = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, itemRowMapper, cartId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartItem> findItemByCartIdAndProductId(UUID cartId, UUID productId) {
        String sql = "SELECT * FROM cart_item WHERE cart_id = ? AND product_id = ?";
        try {
            CartItem item = jdbcTemplate.queryForObject(sql, itemRowMapper, cartId, productId);
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteItemById(UUID itemId) {
        String sql = "DELETE FROM cart_item WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, itemId);
        if (rowsAffected == 0) {
            throw ResourceNotFoundException.forResource("CartItem", itemId);
        }
    }

    @Override
    @Transactional
    public void deleteAllItemsByCartId(UUID cartId) {
        String sql = "DELETE FROM cart_item WHERE cart_id = ?";
        jdbcTemplate.update(sql, cartId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countItemsByCartId(UUID cartId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM cart_item WHERE cart_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cartId);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsItemByCartIdAndProductId(UUID cartId, UUID productId) {
        String sql = "SELECT COUNT(*) FROM cart_item WHERE cart_id = ? AND product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cartId, productId);
        return count != null && count > 0;
    }
}
