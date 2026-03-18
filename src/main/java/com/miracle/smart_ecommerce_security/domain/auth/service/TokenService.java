package com.miracle.smart_ecommerce_security.domain.auth.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for JWT token operations.
 */
public interface TokenService {
    
    /**
     * Generate a JWT token for the given user.
     */
    String generateToken(UUID userId, String role);
    
    /**
     * Validate a token and return the principal if valid.
     */
    Optional<AuthPrincipal> validateToken(String token);
    
    /**
     * Extract the JTI (JWT ID) from a token.
     * Works even on expired or tampered tokens.
     */
    Optional<String> extractJti(String token);

    /**
     * Represents an authenticated principal from a valid token.
     */
    record AuthPrincipal(UUID userId, String role, String jti) {
        public AuthPrincipal(UUID userId, String role) {
            this(userId, role, null);
        }
    }
}

