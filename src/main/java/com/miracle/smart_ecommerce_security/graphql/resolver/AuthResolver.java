package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenBlacklistService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.domain.user.service.UserService;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * GraphQL Resolver for Authentication.
 *
 * Mirrors every operation in AuthController:
 *   Mutation  login(email, password)     → AuthPayload
 *   Mutation  register(input)            → AuthPayload
 *   Mutation  refreshToken(refreshToken) → AuthPayload
 *   Mutation  logout(token)              → Boolean
 *   Query     inspectToken(token)        → TokenIntrospection
 *
 * All auth mutations are public (no @PreAuthorize) because the caller
 * is unauthenticated.  logout requires a valid Bearer token (authenticated).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthResolver {

    private final AuthenticationManager authenticationManager;
    private final UserService           userService;
    private final UserRepository        userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenService       jwtTokenService;

    // ── login ─────────────────────────────────────────────────────────────

    @MutationMapping
    public AuthResponse login(@Argument String email, @Argument String password) {
        log.info("GQL_AUTH_LOGIN — Email: {}", email);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            User user = userRepository.findByEmailAddress(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            AuthResponse response = jwtTokenService.buildAuthResponse(
                    user.getId(), user.getRole(), "graphql", "graphql-client");

            log.info("GQL_AUTH_LOGIN_SUCCESS — UserId: {} — Role: {}", user.getId(), user.getRole());
            return response;

        } catch (BadCredentialsException e) {
            log.warn("GQL_AUTH_LOGIN_FAILED — Email: {}", email);
            throw new RuntimeException("Invalid credentials");
        }
    }

    // ── register ──────────────────────────────────────────────────────────

    @MutationMapping
    public AuthResponse register(@Argument Map<String, Object> input) {
        log.info("GQL_AUTH_REGISTER — Email: {}", input.get("emailAddress"));

        CreateUserRequest request = CreateUserRequest.builder()
                .emailAddress((String) input.get("emailAddress"))
                .firstName((String) input.get("firstName"))
                .lastName((String) input.get("lastName"))
                .phoneNumber((String) input.get("phoneNumber"))
                .password((String) input.get("password"))
                .build();

        UserResponse created = userService.createUser(request);

        AuthResponse response = jwtTokenService.buildAuthResponse(
                created.getId(), created.getRole(), "graphql", "graphql-client");

        log.info("GQL_AUTH_REGISTER_SUCCESS — UserId: {} — Role: {}", created.getId(), created.getRole());
        return response;
    }

    // ── refreshToken ──────────────────────────────────────────────────────

    @MutationMapping
    public AuthResponse refreshToken(@Argument String refreshToken) {
        log.info("GQL_AUTH_REFRESH");
        try {
            AuthResponse response = jwtTokenService.refreshAuthResponse(
                    refreshToken, "graphql", "graphql-client");
            log.info("GQL_AUTH_REFRESH_SUCCESS — UserId: {}", response.getUserId());
            return response;
        } catch (IllegalArgumentException e) {
            log.warn("GQL_AUTH_REFRESH_FAILED — {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // ── logout ────────────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean logout(@Argument String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<String> jti = jwtTokenService.extractJti(token);
        if (jti.isPresent()) {
            tokenBlacklistService.blacklist(jti.get(), Instant.now().plusMillis(86_400_000));
            log.info("GQL_AUTH_LOGOUT — JTI: {}", jti.get());
            return true;
        }
        return false;
    }

    // ── inspectToken ──────────────────────────────────────────────────────

    @QueryMapping
    public JwtTokenService.TokenIntrospection inspectToken(@Argument String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("token argument is required");
        }
        JwtTokenService.TokenIntrospection result = jwtTokenService.introspect(token);
        log.info("GQL_TOKEN_INSPECT — Valid: {} — Subject: {} — Type: {}",
                result.valid(), result.subject(), result.tokenType());
        return result;
    }
}

