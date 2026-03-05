package com.miracle.smart_ecommerce_security.domain.auth.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService.SecurityReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

