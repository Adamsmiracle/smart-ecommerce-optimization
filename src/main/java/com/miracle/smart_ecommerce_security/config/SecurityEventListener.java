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
 * Listens to Spring Security authentication and authorization events for security auditing.
 *
 * <p>This component provides:</p>
 * <ul>
 *   <li>Structured logging for authentication success, failure, and access denial events.</li>
 *   <li>Atomic counters for real-time security statistics (consumed by {@code SecurityReportService}).</li>
 *   <li>A bounded in-memory buffer of recent security events for audit review.</li>
 *   <li>Brute-force detection by tracking consecutive authentication failures per principal.</li>
 * </ul>
 *
 * <p><b>Design Notes:</b></p>
 * <ul>
 *   <li>Uses {@link ConcurrentLinkedDeque} for O(1) insert/remove with bounded size.</li>
 *   <li>Brute-force detection uses {@link ConcurrentHashMap} for O(1) per-principal failure tracking.</li>
 *   <li>All counters are atomic for thread-safe increment operations.</li>
 * </ul>
 */
@Component
@Slf4j
public class SecurityEventListener {

    private static final int MAX_RECENT_EVENTS = 100;
    private static final int BRUTE_FORCE_THRESHOLD = 5;
    
    // Static resource patterns that typically don't require authentication
    private static final List<String> STATIC_RESOURCE_PATTERNS = List.of(
        "favicon", ".ico", ".png", ".css", ".js", ".map", ".woff", ".woff2", ".ttf", ".eot"
    );

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong deniedCount = new AtomicLong();

    private final ConcurrentLinkedDeque<SecurityEvent> recentEvents = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<String, AtomicLong> failuresByPrincipal = new ConcurrentHashMap<>();

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        successCount.incrementAndGet();
        
        // Reset brute-force counter on successful login
        failuresByPrincipal.remove(principal);
        
        addEvent(EventType.AUTH_SUCCESS, principal, "Authorities: " + event.getAuthentication().getAuthorities());
        log.info("AUTH_SUCCESS — Principal: {} — Authorities: {}",
                principal, event.getAuthentication().getAuthorities());
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String principal = event.getAuthentication().getName();
        failureCount.incrementAndGet();
        
        addEvent(EventType.AUTH_FAILURE, principal, "Invalid credentials");
        log.warn("AUTH_FAILURE — Principal: {}", principal);

        // Brute-force detection: track consecutive failures per principal
        long consecutiveFailures = failuresByPrincipal
                .computeIfAbsent(principal, k -> new AtomicLong(0))
                .incrementAndGet();

        if (consecutiveFailures >= BRUTE_FORCE_THRESHOLD) {
            log.warn("BRUTE_FORCE_DETECTED — Principal: {} — ConsecutiveFailures: {}",
                     principal, consecutiveFailures);
            addEvent(EventType.BRUTE_FORCE_DETECTED, principal,
                     "Consecutive failures: " + consecutiveFailures);
        }
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String principal = event.getAuthentication().get().getName();
        String resource = extractResourcePath(event.getSource());

        // Skip logging for unauthenticated requests to static resources
        if (isStaticResourceRequest(principal, resource)) {
            return;
        }

        deniedCount.incrementAndGet();
        addEvent(EventType.ACCESS_DENIED, principal, "Denied: " + event.getAuthorizationDecision());
        log.warn("ACCESS_DENIED — Principal: {} — Resource: {} — Decision: {}",
                principal, resource, event.getAuthorizationDecision());
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
        
        // Maintain bounded size
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.removeLast();
        }
    }

    private String extractResourcePath(Object source) {
        if (source == null) {
            return "unknown";
        }
        String sourceStr = source.toString();
        // Extract the path from the authorization decision
        int pathStart = sourceStr.indexOf("uri=");
        if (pathStart > 0) {
            int pathEnd = sourceStr.indexOf(",", pathStart);
            return pathEnd > 0 ? sourceStr.substring(pathStart + 4, pathEnd) : sourceStr.substring(pathStart + 4);
        }
        return sourceStr;
    }

    private boolean isStaticResourceRequest(String principal, String resource) {
        if (!"anonymousUser".equals(principal)) {
            return false;
        }
        String lowerResource = resource.toLowerCase();
        return STATIC_RESOURCE_PATTERNS.stream().anyMatch(lowerResource::contains);
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
