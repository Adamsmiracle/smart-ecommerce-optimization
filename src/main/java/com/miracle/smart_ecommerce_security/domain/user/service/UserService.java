package com.miracle.smart_ecommerce_security.domain.user.service;

import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for User operations.
 */
public interface UserService {

    /**
     * Create a new user
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Get user by ID
     */
    UserResponse getUserById(UUID id);

    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);

    /**
     * Get all users with pagination
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Search users by keyword
     */
    Page<UserResponse> searchUsers(String keyword, Pageable pageable);

    /**
     * Update user
     */
    UserResponse updateUser(UUID id, UpdateUserRequest request);

    /**
     * Delete user
     */
    void deleteUser(UUID id);

    /**
     * Activate user
     */
    void activateUser(UUID id);

    /**
     * Deactivate user
     */
    void deactivateUser(UUID id);

    /**
     * Count total users
     */
    long countUsers();

    /**
     * Update roles for a user (admin only)
     */
//    void updateUserRoles(UUID id, List<String> roles);
}
