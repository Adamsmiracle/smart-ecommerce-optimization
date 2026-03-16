package com.miracle.smart_ecommerce_security.domain.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.CustomOAuth2UserService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles successful OAuth2/OIDC (Google) authentication.
 *
 * <p>After Google authenticates the user and {@link CustomOAuth2UserService}
 * persists/loads the app User, this handler:</p>
 * <ol>
 *   <li>Calls {@link JwtTokenService#buildAuthResponse} — same method used by
 *       {@code /api/auth/login} — so the response structure is identical.</li>
 *   <li>If {@code app.oauth2.redirect-uri} is set → redirects the browser to the
 *       frontend callback URL with the full token payload as query parameters.</li>
 *   <li>Otherwise → writes the {@link AuthResponse} JSON directly to the response
 *       (Postman / browser direct testing mode).</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService tokenService;
    private final ObjectMapper    objectMapper;

    /**
     * Frontend OAuth2 callback URL.
     * Set in application.yaml:  app.oauth2.redirect-uri: http://localhost:3000/oauth/callback
     * Leave blank to receive raw JSON (default — for Postman / browser testing).
     */
    @Value("${app.oauth2.redirect-uri:}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        User user = oidcUser.getAttribute(CustomOAuth2UserService.APP_USER_ATTRIBUTE);

        if (user == null) {
            log.error("OAUTH2_ERROR — appUser attribute missing from OidcUser principal");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                "{\"status\":false,\"message\":\"OAuth2 authentication failed: user not resolved\"}"
            );
            response.getWriter().flush();
            return;
        }

        String clientIp  = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown";

        // AuthResponse (including refreshToken) built entirely in the service — same as /login
        AuthResponse authResponse = tokenService.buildAuthResponse(
                user.getId(), user.getRole(), clientIp, userAgent);

        log.info("OAUTH2_JWT_ISSUED — UserId: {} — Role: {} — Email: {}",
                user.getId(), user.getRole(), user.getEmailAddress());

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // ── Redirect mode (frontend SPA) ──────────────────────────────────
        if (frontendRedirectUri != null && !frontendRedirectUri.isBlank()) {
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendRedirectUri)
                    .queryParam("token",
                            URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8))
                    .queryParam("refreshToken",
                            URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8))
                    .queryParam("tokenType",  authResponse.getTokenType())
                    .queryParam("expiresIn",  authResponse.getExpiresIn())
                    .queryParam("userId",     authResponse.getUserId().toString())
                    .queryParam("role",       authResponse.getRole())
                    .queryParam("email",
                            URLEncoder.encode(user.getEmailAddress(), StandardCharsets.UTF_8))
                    .build(true)
                    .toUriString();

            log.info("OAUTH2_REDIRECT — UserId: {} — RedirectUri: {}", user.getId(), frontendRedirectUri);
            response.sendRedirect(redirectUrl);
            return;
        }

        // ── JSON mode (Postman / browser direct testing) ──────────────────
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  true);
        body.put("message", "OAuth2 authentication successful");
        body.put("data",    authResponse);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String json = objectMapper.writeValueAsString(body);
        response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
