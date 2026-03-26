package com.miracle.smart_ecommerce_security.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Security filter that extracts and validates JWT tokens from the
 * {@code Authorization: Bearer <token>} header on every request.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>No header present → anonymous request — proceeds to filter chain normally.</li>
 *   <li>Valid Bearer token → populates {@link SecurityContextHolder} with
 *       {@code ROLE_<role>} authority. Downstream {@code @PreAuthorize} checks apply.</li>
 *   <li>Bearer token present but invalid / expired / tampered →
 *       <strong>immediately returns 401 Unauthorized</strong> with a JSON error body.
 *       The filter chain is NOT continued.</li>
 * </ul>
 *
 * This filter is NOT a Spring component — it is instantiated manually in
 * {@link com.miracle.smart_ecommerce_security.config.SecurityConfig} to avoid
 * double-registration by the servlet container.
 */

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;

    public JwtAuthenticationFilter(TokenService tokenService, TokenActivityService tokenActivityService) {
        this.tokenService = tokenService;
        this.tokenActivityService = tokenActivityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Fast-fail for missing or malformed headers
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        Optional<AuthPrincipal> principalOpt = tokenService.validateToken(token);

        if (principalOpt.isEmpty()) {
            // Log failure asynchronously
            String clientIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String jti = tokenService.extractJti(token).orElse("UNKNOWN");
            tokenActivityService.logTokenValidationFailure(jti, "Invalid/expired/tampered JWT", clientIp, userAgent);
            
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        AuthPrincipal auth = principalOpt.get();

        try {
            // Set Security Context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    auth.userId().toString(),
                    null,
                    auth.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Populate MDC for logging context
            MDC.put("userId", auth.userId().toString());
            MDC.put("userRole", auth.role());

            // Async logging - non-blocking
            String clientIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            tokenActivityService.logTokenValidation(auth.jti(), auth.userId().toString(), auth.role(), clientIp, userAgent);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}