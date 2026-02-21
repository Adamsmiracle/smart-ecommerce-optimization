package com.miracle.smart_ecommerce_jpa.domain.user.repository;

import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User domain model.
 * Defines data access operations for users.
 */
public interface UserRepository {

    /**
     * Save a new user
     */
    User save(User user);

    /**
     * Update an existing user
     */
    User update(User user);

    /**
     * Find user by ID
     */
    Optional<User> findById(UUID id);

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users
     */
    List<User> findAll();

    /**
     * Find all users with pagination
     */
    List<User> findAll(int page, int size);

    /**
     * Find active users with pagination
     */
    List<User> findActiveUsers(int page, int size);

    /**
     * Search users by name or email
     */
    List<User> search(String keyword, int page, int size);

    /**
     * Delete user by ID
     */
    void deleteById(UUID id);

    /**
     * Check if user exists by ID
     */
    boolean existsById(UUID id);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count total users
     */
    long count();

    /**
     * Count active users
     */
    long countActive();

    /**
     * Count users matching search keyword
     */
    long countByKeyword(String keyword);

    /**
     * Activate/deactivate user
     */
    void setActiveStatus(UUID id, boolean isActive);
}