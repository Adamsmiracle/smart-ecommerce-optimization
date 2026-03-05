package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.miracle.smart_ecommerce_security.config.SecurityEventListener;
import com.miracle.smart_ecommerce_security.config.SecurityEventListener.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecurityEventListener}.
 *
 * Verifies:
 * - Event counters increment correctly for success, failure, and denied events
 * - Recent events buffer stores events in newest-first order
 * - Buffer is bounded at 100 entries
 */
@DisplayName("SecurityEventListener — Security event tracking and audit buffer")
class SecurityEventListenerTest {

    private SecurityEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SecurityEventListener();
    }

    @Test
    @DisplayName("onAuthenticationSuccess increments success counter and adds event")
    void onSuccess_incrementsCounter() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        var event = new AuthenticationSuccessEvent(auth);

        listener.onAuthenticationSuccess(event);

        assertEquals(1, listener.getSuccessCount());
        assertEquals(0, listener.getFailureCount());
        assertEquals(1, listener.getRecentEvents().size());
        assertEquals(EventType.AUTH_SUCCESS, listener.getRecentEvents().get(0).type());
        assertEquals("admin@test.com", listener.getRecentEvents().get(0).principal());
    }

    @Test
    @DisplayName("onAuthenticationFailure increments failure counter and adds event")
    void onFailure_incrementsCounter() {
        var auth = new UsernamePasswordAuthenticationToken("bad@test.com", "wrongpass");
        var event = new AuthenticationFailureBadCredentialsEvent(
                auth, new BadCredentialsException("Bad credentials")
        );

        listener.onAuthenticationFailure(event);

        assertEquals(0, listener.getSuccessCount());
        assertEquals(1, listener.getFailureCount());
        assertEquals(1, listener.getRecentEvents().size());
        assertEquals(EventType.AUTH_FAILURE, listener.getRecentEvents().get(0).type());
    }

    @Test
    @DisplayName("Multiple events are stored in newest-first order")
    void events_newestFirst() {
        for (int i = 0; i < 5; i++) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "user" + i + "@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
            listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
        }

        assertEquals(5, listener.getSuccessCount());
        var events = listener.getRecentEvents();
        assertEquals(5, events.size());
        // Newest first
        assertEquals("user4@test.com", events.get(0).principal());
        assertEquals("user0@test.com", events.get(4).principal());
    }

    @Test
    @DisplayName("Recent events buffer is bounded at 100 entries")
    void events_boundedBuffer() {
        for (int i = 0; i < 120; i++) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "user" + i + "@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
            listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
        }

        assertEquals(120, listener.getSuccessCount(), "Counter tracks all events");
        assertEquals(100, listener.getRecentEvents().size(), "Buffer should be capped at 100");
        // Oldest 20 should be dropped
        assertEquals("user119@test.com", listener.getRecentEvents().get(0).principal());
    }

    @Test
    @DisplayName("getRecentEvents returns unmodifiable list")
    void events_unmodifiable() {
        var auth = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

        assertThrows(UnsupportedOperationException.class, () -> listener.getRecentEvents().clear());
    }

    @Test
    @DisplayName("Initial counters are zero and events list is empty")
    void initialState() {
        assertEquals(0, listener.getSuccessCount());
        assertEquals(0, listener.getFailureCount());
        assertEquals(0, listener.getDeniedCount());
        assertTrue(listener.getRecentEvents().isEmpty());
    }
}

