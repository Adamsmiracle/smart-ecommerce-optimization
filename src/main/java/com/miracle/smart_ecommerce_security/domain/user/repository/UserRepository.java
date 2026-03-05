package com.miracle.smart_ecommerce_security.domain.user.repository;

import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for User domain model.
 * Extends JpaRepository to provide data access operations for users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     */
    Optional<User> findByEmailAddress(String emailAddress);

    /**
     * Find all active users with pagination
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Page<User> findActiveUsers(Pageable pageable);

    /**
     * Search users by first name, last name, or email with pagination
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Check if email address exists
     */
    boolean existsByEmailAddress(String emailAddress);

    /**
     * Find user by OAuth2 provider and provider-specific ID
     */
    Optional<User> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    /**
     * Activate/deactivate user
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.id = :id")
    void setActiveStatus(@Param("id") UUID id, @Param("isActive") boolean isActive);
}