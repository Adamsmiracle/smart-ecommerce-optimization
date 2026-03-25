package com.miracle.smart_ecommerce_security.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(TokenService tokenService, TokenActivityService tokenActivityService) {
        this.tokenService = tokenService;
        this.tokenActivityService = tokenActivityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token    = header.substring(7);
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(token);

        if (principal.isPresent()) {
            TokenService.AuthPrincipal auth = principal.get();
            String role = auth.role();
            String jti = auth.jti();

            String grantedRole = role.toUpperCase().startsWith("ROLE_")
                    ? role.toUpperCase()
                    : "ROLE_" + role.toUpperCase();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            auth.userId().toString(),
                            null,
                            List.of(new SimpleGrantedAuthority(grantedRole))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            MDC.put("userId", auth.userId().toString());
            MDC.put("userRole", role);

            tokenActivityService.logTokenValidation(jti, auth.userId().toString(), role, clientIp, userAgent);



            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove("userId");
                MDC.remove("userRole");
            }

        } else {
            // Extract JTI for logging if possible
            String jti = tokenService.extractJti(token).orElse(null);
            tokenActivityService.logTokenValidationFailure(jti, "Invalid/expired/tampered JWT",
                    clientIp, userAgent);

            log.warn("JWT_AUTH_FAILED — {} {} — Rejected invalid/expired token — IP: {} — CID: {}",
                    request.getMethod(), request.getRequestURI(), clientIp, MDC.get("correlationId"));

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String body = objectMapper.writeValueAsString(Map.of(
                    "status",     false,
                    "statusCode", 401,
                    "message",    "Authentication failed: token is invalid, expired, or tampered.",
                    "path",       request.getRequestURI(),
                    "timestamp",  java.time.Instant.now().toString()
            ));
            response.setContentLength(body.length());
            response.getWriter().write(body);
            response.getWriter().flush();
            // Do NOT call filterChain.doFilter — stop here
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) return xri;
        return request.getRemoteAddr();
    }
}
