package com.miracle.smart_ecommerce_security.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TokenBlacklistService}.
 *
 * Verifies:
 * - Blacklisting a token makes it immediately detectable via isBlacklisted()
 * - Non-blacklisted tokens are not flagged
 * - purgeExpired() removes only tokens whose expiry is in the past
 * - DSA: ConcurrentHashMap provides O(1) insert and lookup
 */
@DisplayName("TokenBlacklistService — In-memory token revocation (ConcurrentHashMap)")
class TokenBlacklistServiceTest {

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService();
    }

    @Test
    @DisplayName("blacklist + isBlacklisted returns true for blacklisted JTI")
    void blacklist_marksTokenAsBlacklisted() {
        String jti = "test-jti-001";
        service.blacklist(jti, Instant.now().plusSeconds(3600));

        assertTrue(service.isBlacklisted(jti));
        assertEquals(1, service.size());
    }

    @Test
    @DisplayName("isBlacklisted returns false for unknown JTI")
    void isBlacklisted_returnsFalseForUnknown() {
        assertFalse(service.isBlacklisted("unknown-jti"));
    }

    @Test
    @DisplayName("Multiple tokens can be blacklisted independently")
    void blacklist_multipleTokens() {
        service.blacklist("jti-1", Instant.now().plusSeconds(3600));
        service.blacklist("jti-2", Instant.now().plusSeconds(3600));
        service.blacklist("jti-3", Instant.now().plusSeconds(3600));

        assertEquals(3, service.size());
        assertTrue(service.isBlacklisted("jti-1"));
        assertTrue(service.isBlacklisted("jti-2"));
        assertTrue(service.isBlacklisted("jti-3"));
    }

    @Test
    @DisplayName("purgeExpired removes tokens whose expiry is in the past")
    void purgeExpired_removesExpiredTokens() {
        // Expired token (expiry in the past)
        service.blacklist("expired-jti", Instant.now().minusSeconds(10));
        // Still-valid token
        service.blacklist("valid-jti", Instant.now().plusSeconds(3600));

        assertEquals(2, service.size());

        service.purgeExpired();

        assertEquals(1, service.size());
        assertFalse(service.isBlacklisted("expired-jti"), "Expired token should be purged");
        assertTrue(service.isBlacklisted("valid-jti"), "Valid token should remain");
    }

    @Test
    @DisplayName("purgeExpired does nothing when no tokens are expired")
    void purgeExpired_noOpWhenNoneExpired() {
        service.blacklist("jti-a", Instant.now().plusSeconds(3600));
        service.blacklist("jti-b", Instant.now().plusSeconds(7200));

        service.purgeExpired();

        assertEquals(2, service.size());
    }

    @Test
    @DisplayName("size returns 0 for empty blacklist")
    void size_zeroWhenEmpty() {
        assertEquals(0, service.size());
    }
}

