package com.miracle.smart_ecommerce_jpa.domain.auth.service.impl;

import com.miracle.smart_ecommerce_jpa.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_jpa.domain.auth.service.AuthService;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            log.debug("Authentication failed: missing email or password");
            return null;
        }

        String normalizedEmail = email.trim();

        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);
        if (maybeUser.isEmpty()) {
            log.debug("Authentication failed: user not found for email={}", normalizedEmail);
            return null;
        }

        User user = maybeUser.get();

        // If user is inactive, reject authentication
        if (user.getIsActive() != null && !user.getIsActive()) {
            log.debug("Authentication failed: user is inactive id={}", user.getId());
            return null;
        }

        String storedHash = user.getPasswordHash();
        if (storedHash == null) {
            log.debug("Authentication failed: no password hash stored for user id={}", user.getId());
            return null;
        }

        boolean matches = false;
        try {
            matches = passwordEncoder.matches(password, storedHash);
        } catch (Exception ex) {
            log.error("Error while checking password for user id={}: {}", user.getId(), ex.getMessage());
            return null;
        }

        if (!matches) {
            log.debug("Authentication failed: invalid credentials for email={}", normalizedEmail);
            return null;
        }

        String role = user.getRole();
        if (role == null) {
            role = "CUSTOMER"; // fallback default
        }

        log.info("User authenticated: id={}, email={}", user.getId(), normalizedEmail);

        return AuthResponse.builder()
                .userId(user.getId())
                .role(role)
                .build();
    }
}
