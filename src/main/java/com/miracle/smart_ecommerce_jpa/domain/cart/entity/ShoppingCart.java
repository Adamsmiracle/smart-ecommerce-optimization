package com.miracle.smart_ecommerce_jpa.domain.cart.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Shopping Cart domain model (POJO) - represents shopping_cart table.
 * One cart per user.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ShoppingCart {

    @NotNull(message = "id is required")
    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private OffsetDateTime createdAt;

}

