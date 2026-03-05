package com.miracle.smart_ecommerce_security.domain.auth.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
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

