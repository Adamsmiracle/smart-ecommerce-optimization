package com.miracle.smart_ecommerce_jpa.domain.auth.service;

import java.util.Optional;
import java.util.UUID;

public interface TokenService {
    String generateToken(UUID userId, String role);
    Optional<AuthPrincipal> validateToken(String token);

    class AuthPrincipal {
        public final UUID userId;
        public final String role;
        public AuthPrincipal(UUID userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }
}

