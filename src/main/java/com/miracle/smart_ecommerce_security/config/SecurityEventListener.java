package com.miracle.smart_ecommerce_security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listens to Spring Security authentication and authorization events for auditing.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>Structured logging for auth success, failure, and access-denied events.</li>
 *   <li>Atomic counters for real-time statistics (consumed by {@code SecurityReportService}).</li>
 *   <li>A bounded in-memory buffer of the last 100 security events for audit review.</li>
 * </ul>
 *
 * <p><b>DSA note:</b> The recent-events buffer uses a {@link ConcurrentLinkedDeque} capped at
 * 100 entries for O(1) insert/remove at both ends, ensuring bounded memory usage.</p>
 */
@Component
@Slf4j
public class SecurityEventListener {

    private static final int MAX_RECENT_EVENTS = 100;
    /** Brute-force threshold: warn after this many failures for the same principal */
    private static final int BRUTE_FORCE_THRESHOLD = 5;

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong deniedCount  = new AtomicLong();

    private final ConcurrentLinkedDeque<SecurityEvent> recentEvents = new ConcurrentLinkedDeque<>();

    /**
     * DSA: ConcurrentHashMap tracking per-principal failure counts for brute-force detection.
     * Key = principal (email/username), Value = consecutive failure count.
     * O(1) lookup and update.
     */
    private final ConcurrentHashMap<String, AtomicLong> failuresByPrincipal = new ConcurrentHashMap<>();

    // ── Event handlers ────────────────────────────────────────────────────

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        successCount.incrementAndGet();
        // Reset brute-force counter on successful login
        failuresByPrincipal.remove(principal);
        addEvent(EventType.AUTH_SUCCESS, principal, event.getAuthentication().getAuthorities().toString());
        log.info("SECURITY_EVENT — AUTH_SUCCESS — Principal: {} — Authorities: {}",
                principal, event.getAuthentication().getAuthorities());
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String principal = event.getAuthentication().getName();
        failureCount.incrementAndGet();
        addEvent(EventType.AUTH_FAILURE, principal, "Bad credentials");
        log.warn("SECURITY_EVENT — AUTH_FAILURE — Principal: {} — Reason: Bad credentials", principal);

        // Brute-force detection: track consecutive failures per principal
        long consecutiveFailures = failuresByPrincipal
                .computeIfAbsent(principal, k -> new AtomicLong(0))
                .incrementAndGet();

        if (consecutiveFailures >= BRUTE_FORCE_THRESHOLD) {
            log.warn("SECURITY_EVENT — BRUTE_FORCE_DETECTED — Principal: {} — ConsecutiveFailures: {} — " +
                     "ACTION: Consider temporarily blocking this account or IP",
                     principal, consecutiveFailures);
            addEvent(EventType.BRUTE_FORCE_DETECTED, principal,
                     "Consecutive failures: " + consecutiveFailures);
        }
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String principal = event.getAuthentication().get().getName();

        // Suppress noisy ACCESS_DENIED for static browser resources (favicon, etc.)
        // These are requested automatically with no token and are not security events.
        if ("anonymousUser".equals(principal)) {
            Object source = event.getSource();
            if (source != null) {
                String sourceStr = source.toString();
                if (sourceStr.contains("favicon") || sourceStr.contains(".ico")
                        || sourceStr.contains(".png") || sourceStr.contains(".css")
                        || sourceStr.contains(".js")) {
                    return;
                }
            }
        }

        deniedCount.incrementAndGet();
        addEvent(EventType.ACCESS_DENIED, principal, event.getAuthorizationDecision().toString());
        log.warn("SECURITY_EVENT — ACCESS_DENIED — Authentication: {} — Decision: {}",
                principal, event.getAuthorizationDecision());
    }

    // ── Public API for SecurityReportService ──────────────────────────────

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }

    public long getDeniedCount() {
        return deniedCount.get();
    }

    /**
     * Returns an unmodifiable snapshot of recent security events (newest first).
     */
    public List<SecurityEvent> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recentEvents));
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void addEvent(EventType type, String principal, String detail) {
        SecurityEvent event = new SecurityEvent(Instant.now(), type, principal, detail);
        recentEvents.addFirst(event);
        // Trim to bounded size
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.removeLast();
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────

    public enum EventType {
        AUTH_SUCCESS,
        AUTH_FAILURE,
        ACCESS_DENIED,
        BRUTE_FORCE_DETECTED
    }

    public record SecurityEvent(
            Instant timestamp,
            EventType type,
            String principal,
            String detail
    ) {}
}
