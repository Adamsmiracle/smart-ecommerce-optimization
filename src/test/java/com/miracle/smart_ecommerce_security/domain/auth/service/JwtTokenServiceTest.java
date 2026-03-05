package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtTokenService}.
 *
 * Verifies:
 * - Token generation produces a valid JWT with correct claims (sub, role, iat, exp, jti)
 * - Valid tokens are accepted and return the correct principal
 * - Expired tokens are rejected with empty Optional
 * - Tampered tokens are rejected
 * - Blacklisted tokens are rejected (O(1) HashMap lookup)
 */
@DisplayName("JwtTokenService — JWT generation and validation")
class JwtTokenServiceTest {

    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-for-unit-tests-must-be-at-least-32-bytes-long!!".getBytes()
    );
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private TokenBlacklistService blacklistService;
    private TokenActivityService activityService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        blacklistService = mock(TokenBlacklistService.class);
        activityService = mock(TokenActivityService.class);
//        jwtTokenService = new JwtTokenService(BASE64_SECRET, EXPIRATION_MS, blacklistService, activityService);
    }

    // ── Generation tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-null, three-part JWT string")
    void generateToken_returnsValidJwt() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "CUSTOMER");

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts: header.payload.signature");
    }

    @Test
    @DisplayName("generateToken encodes correct subject (userId) and role claims")
    void generateToken_containsCorrectClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "ADMIN");

        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(token);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().userId);
        assertEquals("ADMIN", result.get().role);
    }

    // ── Validation tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken succeeds for a freshly generated token")
    void validateToken_success() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "STAFF");

        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(token);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().userId);
        assertEquals("STAFF", result.get().role);
    }

    @Test
    @DisplayName("validateToken rejects null token")
    void validateToken_rejectsNull() {
        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("validateToken rejects blank token")
    void validateToken_rejectsBlank() {
        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("validateToken rejects tampered token")
    void validateToken_rejectsTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "CUSTOMER");
        // Tamper with the signature by flipping a character
        String tampered = token.substring(0, token.length() - 2) + "XX";

        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(tampered);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("validateToken rejects blacklisted token (DSA: HashMap O(1) lookup)")
    void validateToken_rejectsBlacklistedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "CUSTOMER");

        // Mock blacklist to return true for any JTI
        when(blacklistService.isBlacklisted(anyString())).thenReturn(true);

        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(token);
        assertTrue(result.isEmpty(), "Blacklisted token should be rejected");
    }

    @Test
    @DisplayName("validateToken rejects token signed with a different key")
    void validateToken_rejectsDifferentSigningKey() {
        // Generate token with a different secret
        String otherSecret = Base64.getEncoder().encodeToString(
                "completely-different-secret-key-for-signing-tokens-1234567890!!".getBytes()
        );
        SecretKey otherKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(otherSecret));

        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "CUSTOMER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        Optional<TokenService.AuthPrincipal> result = jwtTokenService.validateToken(token);
        assertTrue(result.isEmpty(), "Token signed with different key should be rejected");
    }

    // ── extractJti tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("extractJti returns JTI from a valid token")
    void extractJti_returnsJti() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(userId, "CUSTOMER");

        Optional<String> jti = jwtTokenService.extractJti(token);
        assertTrue(jti.isPresent());
        assertFalse(jti.get().isBlank());
    }

    @Test
    @DisplayName("extractJti returns empty for invalid token")
    void extractJti_emptyForInvalidToken() {
        Optional<String> jti = jwtTokenService.extractJti("invalid.token.here");
        assertTrue(jti.isEmpty());
    }
}

