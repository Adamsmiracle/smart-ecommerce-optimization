package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for PaymentMethod domain model.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Find all payment methods for a user with pagination
     */
    Page<PaymentMethod> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all active payment methods for a user
     */
    List<PaymentMethod> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find the default payment method for a user
     */
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Set active status for a payment method
     */
    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isActive = :isActive WHERE p.id = :id")
    void setActiveStatus(@Param("id") UUID id, @Param("isActive") boolean isActive);

    /**
     * Set default payment method for a user (clears existing default first)
     */
    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isDefault = false WHERE p.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);

    /**
     * Set a payment method as default
     */
    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isDefault = true WHERE p.id = :id")
    void setAsDefault(@Param("id") UUID id);
}