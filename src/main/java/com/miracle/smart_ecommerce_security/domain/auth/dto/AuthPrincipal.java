package com.miracle.smart_ecommerce_security.domain.auth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Represents an authenticated principal from a valid JWT token.
 * Contains user identity, role, and token identifier.
 */
public record AuthPrincipal(
        UUID userId,
        String role,
        String jti
) {
    /**
     * Convenience constructor for cases where JTI is not available.
     */
    public AuthPrincipal(UUID userId, String role) {
        this(userId, role, null);
    }

    /**
     * Converts the role to Spring Security GrantedAuthority.
     * Ensures the role has the "ROLE_" prefix required by Spring Security.
     *
     * @return Collection of GrantedAuthority with properly formatted role
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String grantedRole = role.toUpperCase().startsWith("ROLE_")
                ? role.toUpperCase()
                : "ROLE_" + role.toUpperCase();
        return List.of(new SimpleGrantedAuthority(grantedRole));
    }
}
