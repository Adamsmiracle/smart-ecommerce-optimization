package com.miracle.smart_ecommerce_jpa.domain.user.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.UserResponse;

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
    PageResponse<UserResponse> getAllUsers(int page, int size);

    /**
     * Search users by keyword
     */
    PageResponse<UserResponse> searchUsers(String keyword, int page, int size);

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
