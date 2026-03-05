package com.miracle.smart_ecommerce_security.domain.cart.entity;

import com.miracle.smart_ecommerce_security.domain.BaseModel;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Shopping Cart JPA entity - represents shopping_cart table.
 * One cart per user.
 */
@Entity
@Table(name = "shopping_cart")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ShoppingCart extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<CartItem> cartItems = new ArrayList<>();
}
