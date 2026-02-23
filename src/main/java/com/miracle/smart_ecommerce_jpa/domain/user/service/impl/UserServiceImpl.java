package com.miracle.smart_ecommerce_jpa.domain.user.service.impl;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.UpdateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.UserService;
import com.miracle.smart_ecommerce_jpa.exception.DuplicateResourceException;
import com.miracle.smart_ecommerce_jpa.exception.ResourceNotFoundException;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of UserService using Spring Data JPA.
 *
 * Transaction strategy:
 * - Read operations use readOnly = true for performance optimization
 * - Write operations use default REQUIRED propagation
 * - @Modifying queries (setActiveStatus) run within the same transaction
 *
 * Cache strategy:
 * - Users cached by ID and email for fast lookup
 * - All entries evicted on delete, activate, deactivate, and update
 *   to avoid stale data especially when email changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user.
     * Checks for duplicate email before saving.
     * Password is hashed before persistence.
     * Result is cached by ID and email after creation.
     */
    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = USERS_CACHE, key = "'id:' + #result.id"),
            @CachePut(value = USERS_CACHE, key = "'email:' + #result.emailAddress")
    })
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmailAddress());

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (userRepository.existsByEmailAddress(request.getEmailAddress())) {
            throw new DuplicateResourceException("User", "email", request.getEmailAddress());
        }

        User user = User.builder()
                .emailAddress(request.getEmailAddress())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(hashPassword(request.getPassword()))
                .isActive(true)
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Get user by ID.
     * Result is cached by ID to avoid repeated DB lookups.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_CACHE, key = "'id:' + #id")
    public UserResponse getUserById(UUID id) {
        log.debug("Getting user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));
        return mapToResponse(user);
    }

    /**
     * Get user by email address.
     * Result is cached by email to avoid repeated DB lookups.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_CACHE, key = "'email:' + #email")
    public UserResponse getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToResponse(user);
    }

    /**
     * Get all users with pagination and sorting.
     * Not cached due to dynamic nature of pageable parameters.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Getting all users - pageable: {}", pageable);

        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> responses = userPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), userPage.getTotalElements());
    }

    /**
     * Search users by keyword across first name, last name, and email with pagination.
     * Not cached due to dynamic nature of keyword and pageable parameters.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        log.debug("Searching users with keyword: {}", keyword);

        Page<User> userPage = userRepository.search(keyword, pageable);

        List<UserResponse> responses = userPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, pageable.getPageNumber(), pageable.getPageSize(), userPage.getTotalElements());
    }

    /**
     * Update an existing user.
     * Uses JPA dirty checking — no explicit save() needed after field mutation.
     * All cache entries evicted to handle email key changes cleanly.
     */
    @Override
    @Transactional
    @CacheEvict(value = USERS_CACHE, allEntries = true)
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));

        // Check if email is being changed to one already in use
        if (!user.getEmailAddress().equals(request.getEmailAddress())
                && userRepository.existsByEmailAddress(request.getEmailAddress())) {
            throw new DuplicateResourceException("User", "email", request.getEmailAddress());
        }

        user.setEmailAddress(request.getEmailAddress());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        log.info("User updated successfully: {}", id);
        return mapToResponse(user);
    }

    /**
     * Delete a user by ID.
     * All cache entries evicted after deletion.
     */
    @Override
    @Transactional
    @CacheEvict(value = USERS_CACHE, allEntries = true)
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));
        userRepository.delete(user);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * Activate a user account.
     * Uses existsById to avoid loading the full entity unnecessarily.
     */
    @Override
    @Transactional
    @CacheEvict(value = USERS_CACHE, allEntries = true)
    public void activateUser(UUID id) {
        log.info("Activating user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("User", id);
        }
        userRepository.setActiveStatus(id, true);
        log.info("User activated successfully: {}", id);
    }

    /**
     * Deactivate a user account.
     * Uses existsById to avoid loading the full entity unnecessarily.
     */
    @Override
    @Transactional
    @CacheEvict(value = USERS_CACHE, allEntries = true)
    public void deactivateUser(UUID id) {
        log.info("Deactivating user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException.forResource("User", id);
        }
        userRepository.setActiveStatus(id, false);
        log.info("User deactivated successfully: {}", id);
    }

    /**
     * Count total users.
     */
    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Verify a raw password against a stored hashed password.
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .emailAddress(user.getEmailAddress())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(user.getRole())
                .build();
    }

    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty for hashing");
        }
        return passwordEncoder.encode(password);
    }
}