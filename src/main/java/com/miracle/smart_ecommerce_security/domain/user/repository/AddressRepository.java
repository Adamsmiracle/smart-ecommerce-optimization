package com.miracle.smart_ecommerce_security.domain.user.repository;

import com.miracle.smart_ecommerce_security.domain.user.entity.Address;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
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
 * JPA Repository interface for Address operations using User entity reference.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find all addresses for a user
     */
    List<Address> findByUser(User user);

    /**
     * Find addresses by user and type
     */
    List<Address> findByUserAndAddressType(User user, String addressType);

    /**
     * Pageable variants for controller/service pagination
     */
    Page<Address> findByUser(User user, Pageable pageable);

    Page<Address> findByUserAndAddressType(User user, String addressType, Pageable pageable);

    /**
     * Find the default address for a user
     */
    Optional<Address> findByUserAndIsDefaultTrue(User user);

    /**
     * Clear default flag for all addresses of a user
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user")
    void clearDefaultByUser(@Param("user") User user);

    /**
     * Set an address as default
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = true WHERE a.id = :id")
    void setAsDefault(@Param("id") UUID id);
}