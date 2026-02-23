package com.miracle.smart_ecommerce_jpa.domain.order.repository;

import com.miracle.smart_ecommerce_jpa.domain.order.entity.ShippingMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository interface for ShippingMethod domain model.
 */
@Repository
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {

    /**
     * Find all shipping methods with pagination
     */
    Page<ShippingMethod> findAll(Pageable pageable);

    /**
     * Find all active shipping methods
     */
    List<ShippingMethod> findByIsActiveTrue();

    /**
     * Set active status for a shipping method
     */
    @Modifying
    @Query("UPDATE ShippingMethod s SET s.isActive = :isActive WHERE s.id = :id")
    void setActiveStatus(@Param("id") UUID id, @Param("isActive") boolean isActive);
}