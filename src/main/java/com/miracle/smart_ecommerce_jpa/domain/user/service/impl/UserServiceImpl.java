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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.miracle.smart_ecommerce_jpa.config.CacheConfig.*;

/**
 * Implementation of UserService using raw JDBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmailAddress());

        // Debug logging
        log.debug("CreateUserRequest details - email: {}, firstName: {}, lastName: {}, phoneNumber: {}, password provided: {}",
            request.getEmailAddress(),
            request.getFirstName(),
            request.getLastName(),
            request.getPhoneNumber(),
            request.getPassword() != null && !request.getPassword().isEmpty());

        // Validate password is not null
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            log.error("Password validation failed - password is null or empty");
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmailAddress())) {
            throw new DuplicateResourceException("User", "email", request.getEmailAddress());
        }

        String hashedPassword = hashPassword(request.getPassword());
        log.debug("Password hashed successfully, length: {}", hashedPassword.length());

        // Verify hashedPassword is not null before building user
        if (hashedPassword == null) {
            log.error("Hashed password is null after hashing!");
            throw new IllegalStateException("Password hashing failed - result is null");
        }

        User user = User.builder()
                .emailAddress(request.getEmailAddress())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(hashedPassword)
                .isActive(true)
                .role(request.getRole())
                .build();


        // Verify user object before saving
        log.debug("User object before save - passwordHash is null: {}, isActive: {}",
            user.getPasswordHash() == null,
            user.getIsActive());

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        UserResponse response = mapToResponse(savedUser);

        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.put("id:" + savedUser.getId(), response);
            cache.put("email:" + savedUser.getEmailAddress(), response);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_CACHE, key = "'id:' + #id")
    public UserResponse getUserById(UUID id) {
        log.debug("Getting user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_CACHE, key = "'email:' + #email")
    public UserResponse getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        log.debug("Getting all users - page: {}, size: {}", page, size);
        List<User> users = userRepository.findAll(page, size);
        long total = userRepository.count();

        List<UserResponse> responses = users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, int page, int size) {
        log.debug("Searching users with keyword: {} - page: {}, size: {}", keyword, page, size);
        List<User> users = userRepository.search(keyword, page, size);

        // FIXED: Count only users matching the keyword, not all users
        long total = userRepository.countByKeyword(keyword);

        List<UserResponse> responses = users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, size, total);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));

        String oldEmail = existingUser.getEmailAddress();

        // Check if email is being changed to an existing one
        if (!existingUser.getEmailAddress().equals(request.getEmailAddress())
                && userRepository.existsByEmail(request.getEmailAddress())) {
            throw new DuplicateResourceException("User", "email", request.getEmailAddress());
        }

        existingUser.setEmailAddress(request.getEmailAddress());
        existingUser.setFirstName(request.getFirstName());
        existingUser.setLastName(request.getLastName());
        existingUser.setPhoneNumber(request.getPhoneNumber());
        // Update role only if provided
        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        // Password updates are not allowed via UpdateUserRequest

        User updatedUser = userRepository.update(existingUser);
        log.info("User updated successfully: {}", id);

        UserResponse response = mapToResponse(updatedUser);

        // Update cache - both id and email keys
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            // Evict old email key if changed
            if (!oldEmail.equals(updatedUser.getEmailAddress())) {
                cache.evict("email:" + oldEmail);
            }
            // Put updated data under both keys
            cache.put("id:" + id, response);
            cache.put("email:" + updatedUser.getEmailAddress(), response);
        }

        return response;
    }


    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", id));

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);

        // Evict from cache (both id and email keys)
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.evict("id:" + id);
            cache.evict("email:" + user.getEmailAddress());
        }
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        log.info("Activating user with ID: {}", id);
        userRepository.setActiveStatus(id, true);

        // Evict cache - user status changed
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.evict("id:" + id);
            // Get user to evict email cache too
            userRepository.findById(id).ifPresent(user ->
                cache.evict("email:" + user.getEmailAddress())
            );
        }
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user with ID: {}", id);
        userRepository.setActiveStatus(id, false);

        // Evict cache - user status changed
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.evict("id:" + id);
            // Get user to evict email cache too
            userRepository.findById(id).ifPresent(user ->
                cache.evict("email:" + user.getEmailAddress())
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
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
        log.debug("Hashing password with BCrypt");
        return passwordEncoder.encode(password);
    }


    /**
     * Verify a password against a hashed password
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
