package com.miracle.smart_ecommerce_jpa.domain.user.repository;

import com.miracle.smart_ecommerce_jpa.domain.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository interface for Address operations.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find all addresses for a user
     */
    List<Address> findByUserId(UUID userId);

    /**
     * Find addresses by user ID and type
     */
    List<Address> findByUserIdAndAddressType(UUID userId, String addressType);

    /**
     * Find the default address for a user
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Clear default flag for all addresses of a user
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);

    /**
     * Set an address as default
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = true WHERE a.id = :id")
    void setAsDefault(@Param("id") UUID id);
}