package com.miracle.smart_ecommerce_security.domain.auth.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService.SecurityReport;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin-only endpoint exposing security audit reports.
 *
 * <p>Returns authentication statistics, token usage metrics, and recent security events
 * for monitoring login attempts, access patterns, and potential brute-force attacks.</p>
 *
 * <p>Protected by both URL-based ({@code /api/admin/**} → ADMIN) and method-level
 * ({@code @PreAuthorize}) access control for defense-in-depth.</p>
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Security", description = "Admin-only security audit and reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SecurityReportController {

    private final SecurityReportService securityReportService;
    private final TokenPerformanceService tokenPerformanceService;

    @GetMapping("/security-report")
    @Operation(
            summary = "Get security audit report",
            description = "Returns authentication statistics (success/failure/denied counts), " +
                    "token usage metrics (active tokens, blacklist size), and the last 100 security events. " +
                    "Use this to detect brute-force attempts and unusual access patterns."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges (ADMIN required)")
    })
    public ResponseEntity<ApiResponse<SecurityReport>> getSecurityReport() {
        SecurityReport report = securityReportService.generateReport();
        return ResponseEntity.ok(ApiResponse.success(report, "Security report generated successfully"));
    }

    @GetMapping("/token-performance")
    @Operation(
            summary = "Get token validation performance metrics",
            description = "Returns cache hit rates, validation times, and performance indicators for JWT token validation. " +
                    "Use this to monitor and optimize token validation performance."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Metrics retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges (ADMIN required)")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenPerformance() {
        Map<String, Object> metrics = tokenPerformanceService.getTokenCacheMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "Token performance metrics retrieved"));
    }

    @GetMapping("/token-cache-capacity")
    @Operation(
            summary = "Get token cache capacity information",
            description = "Returns current cache size, maximum capacity, and utilization percentage."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenCacheCapacity() {
        Map<String, Object> info = tokenPerformanceService.getCacheCapacityInfo();
        return ResponseEntity.ok(ApiResponse.success(info, "Cache capacity info retrieved"));
    }

    @DeleteMapping("/token-cache")
    @Operation(
            summary = "Clear token cache",
            description = "Clears all cached token validations. Use this for testing or emergency scenarios. " +
                    "Note: This will temporarily increase validation latency until cache warms up again."
    )
    public ResponseEntity<ApiResponse<Void>> clearTokenCache() {
        tokenPerformanceService.clearTokenCache();
        return ResponseEntity.ok(ApiResponse.success(null, "Token cache cleared successfully"));
    }
}

