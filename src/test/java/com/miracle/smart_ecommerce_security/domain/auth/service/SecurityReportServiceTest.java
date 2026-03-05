package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.miracle.smart_ecommerce_security.config.SecurityEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SecurityReportService}.
 *
 * Verifies the report aggregates data correctly from all three sources:
 * SecurityEventListener, TokenBlacklistService, and TokenActivityService.
 */
@DisplayName("SecurityReportService — Security audit report aggregation")
class SecurityReportServiceTest {

    private SecurityEventListener eventListener;
    private TokenBlacklistService blacklistService;
    private TokenActivityService activityService;
    private SecurityReportService reportService;

    @BeforeEach
    void setUp() {
        eventListener = mock(SecurityEventListener.class);
        blacklistService = mock(TokenBlacklistService.class);
        activityService = mock(TokenActivityService.class);
        reportService = new SecurityReportService(eventListener, blacklistService, activityService);
    }

    @Test
    @DisplayName("generateReport aggregates all metrics into a single report")
    void generateReport_aggregatesAllSources() {
        when(eventListener.getSuccessCount()).thenReturn(42L);
        when(eventListener.getFailureCount()).thenReturn(5L);
        when(eventListener.getDeniedCount()).thenReturn(3L);
        when(eventListener.getRecentEvents()).thenReturn(java.util.List.of());
        when(blacklistService.size()).thenReturn(7);
        when(activityService.getTokenUsageStats()).thenReturn(
                new TokenActivityService.TokenUsageStats(15, 200)
        );

        SecurityReportService.SecurityReport report = reportService.generateReport();

        assertNotNull(report);
        assertNotNull(report.generatedAt());

        // Auth stats
        assertEquals(42, report.authenticationStats().totalSuccessfulLogins());
        assertEquals(5, report.authenticationStats().totalFailedLogins());
        assertEquals(3, report.authenticationStats().totalAccessDenials());

        // Token stats
        assertEquals(15, report.tokenStats().activeTokens());
        assertEquals(200, report.tokenStats().totalValidations());
        assertEquals(7, report.tokenStats().blacklistedTokens());

        // Recent events
        assertTrue(report.recentSecurityEvents().isEmpty());
    }

    @Test
    @DisplayName("generateReport includes recent events from event listener")
    void generateReport_includesRecentEvents() {
        var mockEvent = new SecurityEventListener.SecurityEvent(
                java.time.Instant.now(),
                SecurityEventListener.EventType.AUTH_SUCCESS,
                "admin@test.com",
                "[ROLE_ADMIN]"
        );
        when(eventListener.getSuccessCount()).thenReturn(1L);
        when(eventListener.getFailureCount()).thenReturn(0L);
        when(eventListener.getDeniedCount()).thenReturn(0L);
        when(eventListener.getRecentEvents()).thenReturn(java.util.List.of(mockEvent));
        when(blacklistService.size()).thenReturn(0);
        when(activityService.getTokenUsageStats()).thenReturn(
                new TokenActivityService.TokenUsageStats(1, 1)
        );

        SecurityReportService.SecurityReport report = reportService.generateReport();

        assertEquals(1, report.recentSecurityEvents().size());
        assertEquals("admin@test.com", report.recentSecurityEvents().get(0).principal());
    }
}

