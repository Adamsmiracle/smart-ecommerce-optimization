package com.miracle.smart_ecommerce_jpa.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.*;
import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;

/**
 * User JPA entity - represents app_user table.
 */
@Entity
@Table(name = "app_user",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email_address"),
                @Index(name = "idx_user_role", columnList = "role")
        })
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseModel {

    @NotBlank(message = "Email address is required")
    @Email(message = "Email should be valid", regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Column(name = "email_address", nullable = false, unique = true, length = 255)
    private String emailAddress;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Column(name = "password_hash", nullable = false)
    @JsonProperty(access = Access.WRITE_ONLY)
    private String passwordHash;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "role", length = 50, nullable = false)
    private String role = "CUSTOMER";

    /**
     * Get user's full name
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

}
