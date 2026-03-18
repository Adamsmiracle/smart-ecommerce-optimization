package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.miracle.smart_ecommerce_security.config.SecurityEventListener;
import com.miracle.smart_ecommerce_security.config.SecurityEventListener.SecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Aggregates security metrics from multiple sources to produce an admin-facing audit report.
 *
 * <p>Data sources:</p>
 * <ul>
 *   <li>{@link SecurityEventListener} — auth success/failure/denied counters and recent events</li>
 *   <li>{@link TokenBlacklistService} — count of blacklisted (revoked) tokens</li>
 * </ul>
 *
 * <p>Note: Active token tracking was removed as JWT tokens are stateless by design -
 * the token itself is the source of truth. Token validity is determined by cryptographic
 * signature verification and expiry, not by server-side state.</p>
 *
 * <p><b>DSA note:</b> All data sources use thread-safe structures (AtomicLong, ConcurrentHashMap,
 * ConcurrentLinkedDeque) so this service can safely aggregate without locks.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityReportService {

    private final SecurityEventListener securityEventListener;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Build a comprehensive security report for admin review.
     */
    public SecurityReport generateReport() {
        log.info("SECURITY_REPORT — Generating admin security report");

        return new SecurityReport(
                Instant.now(),
                new AuthStats(
                        securityEventListener.getSuccessCount(),
                        securityEventListener.getFailureCount(),
                        securityEventListener.getDeniedCount()
                ),
                new TokenStats(
                        tokenBlacklistService.size()
                ),
                securityEventListener.getRecentEvents()
        );
    }

    // ── Report DTOs ───────────────────────────────────────────────────────

    public record SecurityReport(
            Instant generatedAt,
            AuthStats authenticationStats,
            TokenStats tokenStats,
            List<SecurityEvent> recentSecurityEvents
    ) {}

    public record AuthStats(
            long totalSuccessfulLogins,
            long totalFailedLogins,
            long totalAccessDenials
    ) {}

    public record TokenStats(
            int blacklistedTokens
    ) {}
}

