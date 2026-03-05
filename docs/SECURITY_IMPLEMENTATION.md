# Smart E-Commerce Security — Complete Implementation Reference

> All source files, tests, and configuration for the Spring Security implementation.  
> **Build status:** ✅ 25 tests pass | Maven BUILD SUCCESS  
> **Stack:** Spring Boot 3.3.5 · Spring Security 6 · Java 21 · JJWT 0.12.6 · OAuth2 (Google) · PostgreSQL

---

## Table of Contents

- [Project Structure](#project-structure)
- [Epic 1 — Security Configuration and Access Policies](#epic-1--security-configuration-and-access-policies)
  - [SecurityConfig.java](#securityconfigjava)
  - [PasswordConfig.java](#passwordconfigjava)
- [Epic 2 — JWT-Based Authentication](#epic-2--jwt-based-authentication)
  - [TokenService.java (interface)](#tokenservicejava-interface)
  - [JwtTokenService.java](#jwttokenservicejava)
  - [JwtAuthenticationFilter.java](#jwtauthenticationfilterjava)
  - [AuthController.java](#authcontrollerjava)
  - [AuthRequest.java / AuthResponse.java (DTOs)](#authrequestjava--authresponsejava-dtos)
- [Epic 3 — CSRF and Session Security](#epic-3--csrf-and-session-security)
  - [CsrfDemoController.java](#csrfdemocontrollerjava)
  - [csrf-demo.html (Thymeleaf template)](#csrf-demohtml-thymeleaf-template)
- [Epic 4 — OAuth2 and Role-Based Access Control](#epic-4--oauth2-and-role-based-access-control)
  - [CustomOAuth2UserService.java](#customoauth2userservicejava)
  - [OAuth2AuthenticationSuccessHandler.java](#oauth2authenticationsuccesshandlerjava)
  - [CustomUserDetailsService.java](#customuserdetailsservicejava)
  - [GraphQL Resolvers (@PreAuthorize migration)](#graphql-resolvers-preauthorize-migration)
- [Epic 5 — DSA and Security Optimization](#epic-5--dsa-and-security-optimization)
  - [TokenBlacklistService.java](#tokenblacklistservicejava)
  - [TokenActivityService.java](#tokenactivityservicejava)
  - [SecurityEventListener.java](#securityeventlistenerjava)
  - [SecurityReportService.java](#securityreportservicejava)
  - [SecurityReportController.java](#securityreportcontrollerjava)
- [Configuration](#configuration)
  - [application.yaml (security-related)](#applicationyaml-security-related)
  - [V2__Add_oauth_provider_columns.sql](#v2__add_oauth_provider_columnssql)
- [Unit Tests (25 passing)](#unit-tests-25-passing)
  - [JwtTokenServiceTest.java](#jwttokenservicetestjava)
  - [TokenBlacklistServiceTest.java](#tokenblacklistservicetestjava)
  - [SecurityEventListenerTest.java](#securityeventlistenertestjava)
  - [SecurityReportServiceTest.java](#securityreportservicetestjava)
- [Deleted Legacy Files](#deleted-legacy-files)
- [Change Summary](#change-summary)

---

## Project Structure

```
src/main/java/com/miracle/smart_ecommerce_security/
├── config/
│   ├── SecurityConfig.java              ← Central security filter chain
│   ├── PasswordConfig.java              ← BCryptPasswordEncoder bean
│   └── SecurityEventListener.java       ← Auth event tracking + audit buffer
├── domain/auth/
│   ├── controller/
│   │   ├── AuthController.java          ← /api/auth/login, /register, /logout
│   │   ├── CsrfDemoController.java      ← CSRF demo form (Thymeleaf)
│   │   └── SecurityReportController.java← Admin security report endpoint
│   ├── dto/
│   │   ├── AuthRequest.java             ← Login request DTO
│   │   └── AuthResponse.java            ← Login response DTO
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java ← Bearer token extraction + validation
│   ├── handler/
│   │   └── OAuth2AuthenticationSuccessHandler.java ← JWT after OAuth2 login
│   └── service/
│       ├── TokenService.java            ← Token interface
│       ├── TokenBlacklistService.java   ← ConcurrentHashMap blacklist (DSA)
│       ├── TokenActivityService.java    ← Token usage tracking
│       ├── SecurityReportService.java   ← Audit report aggregation
│       └── impl/
│           ├── JwtTokenService.java     ← HMAC-SHA256 JWT implementation
│           ├── CustomUserDetailsService.java ← DB user loader
│           └── CustomOAuth2UserService.java  ← Google OAuth2 user provisioning
├── graphql/resolver/
│   ├── AddressResolver.java             ← @PreAuthorize (migrated from @RequireRoles)
│   ├── CartResolver.java                ← @PreAuthorize
│   ├── CategoryResolver.java            ← @PreAuthorize
│   ├── OrderResolver.java               ← @PreAuthorize
│   ├── ProductResolver.java             ← @PreAuthorize
│   ├── ReviewResolver.java              ← @PreAuthorize
│   └── UserResolver.java                ← @PreAuthorize
└── annotation/
    └── RequireRoles.java                ← @Deprecated (replaced by @PreAuthorize)

src/main/resources/
├── templates/
│   └── csrf-demo.html                   ← CSRF demo Thymeleaf page
└── application.yaml                     ← JWT + OAuth2 config

src/test/java/com/miracle/smart_ecommerce_security/domain/auth/service/
├── JwtTokenServiceTest.java             ← 11 tests
├── TokenBlacklistServiceTest.java       ← 6 tests
├── SecurityEventListenerTest.java       ← 6 tests
└── SecurityReportServiceTest.java       ← 2 tests
```

---

## Epic 1 — Security Configuration and Access Policies

### SecurityConfig.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/config/SecurityConfig.java`

```java
package com.miracle.smart_ecommerce_security.config;

import com.miracle.smart_ecommerce_security.domain.auth.filter.JwtAuthenticationFilter;
import com.miracle.smart_ecommerce_security.domain.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.CustomOAuth2UserService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled for stateless JWT API paths; enabled for /csrf-demo form
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/graphql")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/graphql", "/graphiql/**", "/graphiql").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/login/**", "/oauth2/**").permitAll()
                .requestMatchers("/csrf-demo/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            )
            .authenticationProvider(daoAuthenticationProvider())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"status\":false,\"message\":\"Authentication required. Provide a valid Bearer JWT token.\",\"statusCode\":401}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(403);
                    response.getWriter().write(
                        "{\"status\":false,\"message\":\"Access denied. Insufficient role privileges.\",\"statusCode\":403}"
                    );
                })
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenService, tokenActivityService);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000", "http://localhost:3001", "http://localhost:3002",
                "http://localhost:4200", "http://localhost:5173", "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Correlation-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### PasswordConfig.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/config/PasswordConfig.java`

```java
package com.miracle.smart_ecommerce_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Epic 2 — JWT-Based Authentication

### TokenService.java (interface)
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/TokenService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service;

import java.util.Optional;
import java.util.UUID;

public interface TokenService {
    String generateToken(UUID userId, String role);
    Optional<AuthPrincipal> validateToken(String token);

    class AuthPrincipal {
        public final UUID userId;
        public final String role;
        public AuthPrincipal(UUID userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }
}
```

### JwtTokenService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/impl/JwtTokenService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.auth.service.TokenBlacklistService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT implementation using HMAC-SHA256 (HS256).
 * Claims: sub (userId), role, iat, exp, jti.
 * DSA: HMAC hashing for signatures + ConcurrentHashMap O(1) blacklist lookup.
 */
@Service
@Slf4j
public class JwtTokenService implements TokenService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final TokenBlacklistService blacklistService;
    private final TokenActivityService tokenActivityService;

    public JwtTokenService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            TokenBlacklistService blacklistService,
            TokenActivityService tokenActivityService) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.blacklistService = blacklistService;
        this.tokenActivityService = tokenActivityService;
        log.info("JwtTokenService initialised — algorithm: HS256, expiry: {}ms", expirationMs);
    }

    @Override
    public String generateToken(UUID userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(jti)
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        log.info("JWT_GENERATED — UserId: {} — Role: {} — JTI: {} — Expiry: {} — CID: {}",
                userId, role, jti, expiry, MDC.get("correlationId"));

        if (tokenActivityService != null) {
            tokenActivityService.logTokenGeneration(jti, userId.toString(), role, "unknown", "unknown");
        }
        return token;
    }

    @Override
    public Optional<AuthPrincipal> validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("JWT_VALIDATION_FAILED — Token is null or empty");
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            String userIdStr = claims.getSubject();
            String role = claims.get("role", String.class);

            // Check blacklist (DSA: HashMap lookup O(1))
            if (jti != null && blacklistService.isBlacklisted(jti)) {
                log.warn("JWT_VALIDATION_FAILED — Token is blacklisted — JTI: {}", jti);
                return Optional.empty();
            }

            UUID userId = UUID.fromString(userIdStr);
            log.info("JWT_VALIDATION_SUCCESS — UserId: {} — Role: {} — JTI: {}", userId, role, jti);
            return Optional.of(new AuthPrincipal(userId, role));

        } catch (ExpiredJwtException ex) {
            log.warn("JWT_VALIDATION_FAILED — Token expired — Sub: {}", ex.getClaims().getSubject());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT_VALIDATION_FAILED — {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractJti(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
```

### JwtAuthenticationFilter.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/filter/JwtAuthenticationFilter.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.filter;

import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Extracts Bearer JWT from Authorization header, validates it, and populates
 * SecurityContextHolder with ROLE_<role> authorities.
 * NOT a @Component — instantiated manually in SecurityConfig.
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

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String clientIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(token);

            if (principal.isPresent()) {
                TokenService.AuthPrincipal auth = principal.get();
                String role = auth.role;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                auth.userId.toString(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                MDC.put("userId", auth.userId.toString());
                MDC.put("userRole", role);

                tokenActivityService.logTokenValidation(token, auth.userId.toString(), role, clientIp, userAgent);
                log.debug("JWT_AUTH_SUCCESS — {} {} — UserId: {} — Role: {} — IP: {}",
                        request.getMethod(), request.getRequestURI(), auth.userId, role, clientIp);
            } else {
                tokenActivityService.logTokenValidationFailure(token, "Invalid/expired JWT", clientIp, userAgent);
                log.warn("JWT_AUTH_FAILED — {} {} — Invalid token — IP: {}",
                        request.getMethod(), request.getRequestURI(), clientIp);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("userRole");
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
```

### AuthController.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/controller/AuthController.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthRequest;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenBlacklistService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@Tag(name = "Authentication", description = "Login, registration, and logout endpoints")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenService jwtTokenService;

    // Constructor injection (all 7 dependencies)...

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody AuthRequest request,
                                                                   HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmailAddress(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            String token = tokenService.generateToken(user.getId(), user.getRole());
            AuthResponse response = AuthResponse.builder()
                    .userId(user.getId()).role(user.getRole()).token(token).build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials", 401));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody CreateUserRequest request) {
        var created = userService.createUser(request);
        String token = tokenService.generateToken(created.getId(), created.getRole());
        AuthResponse response = AuthResponse.builder()
                .userId(created.getId()).role(created.getRole()).token(token).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<String> jti = jwtTokenService.extractJti(token);
            if (jti.isPresent()) {
                tokenBlacklistService.blacklist(jti.get(), Instant.now().plusMillis(86_400_000));
                tokenActivityService.logTokenRevocation(token, "User logout", getClientIp(httpRequest));
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
```

### AuthRequest.java / AuthResponse.java (DTOs)
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/dto/`

```java
// AuthRequest.java
@Data
public class AuthRequest {
    @Email @NotNull(message = "email is required")
    private String email;

    @NotNull(message = "password is required")
    private String password;
}

// AuthResponse.java
@Data @Builder
public class AuthResponse {
    private UUID userId;
    private String role;
    private String token;
}
```

---

## Epic 3 — CSRF and Session Security

### CsrfDemoController.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/controller/CsrfDemoController.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Demonstrates CSRF protection for stateful form-based interactions.
 * 
 * - /api/** and /graphql → CSRF DISABLED (stateless JWT, no cookies)
 * - /csrf-demo → CSRF ENABLED (this Thymeleaf form demonstrates it)
 * 
 * When to enable CSRF:
 * - Enable: cookie/session-based auth + HTML forms
 * - Disable: stateless JWT API endpoints (Authorization: Bearer)
 */
@Controller
@RequestMapping("/csrf-demo")
@Hidden
@Slf4j
public class CsrfDemoController {

    @GetMapping
    public String showForm(Model model, HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("csrfTokenValue", csrfToken.getToken());
            model.addAttribute("csrfHeaderName", csrfToken.getHeaderName());
            model.addAttribute("csrfParameterName", csrfToken.getParameterName());
        }
        model.addAttribute("submitted", false);
        return "csrf-demo";
    }

    @PostMapping
    public String handleSubmit(@RequestParam String message, Model model, HttpServletRequest request) {
        log.info("CSRF_DEMO — Form submitted successfully — Message: '{}' — IP: {}",
                message, request.getRemoteAddr());

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("csrfTokenValue", csrfToken.getToken());
            model.addAttribute("csrfHeaderName", csrfToken.getHeaderName());
            model.addAttribute("csrfParameterName", csrfToken.getParameterName());
        }
        model.addAttribute("submitted", true);
        model.addAttribute("submittedMessage", message);
        return "csrf-demo";
    }
}
```

### csrf-demo.html (Thymeleaf template)
**Path:** `src/main/resources/templates/csrf-demo.html`

Renders:
- **CSRF vs CORS comparison table**
- **Live CSRF token value** (generated by Spring Security)
- **A form** with `th:action` that auto-injects the hidden `_csrf` field
- **Submission result** showing the token was validated

> Visit `http://localhost:8080/csrf-demo` to see it in action.

---

## Epic 4 — OAuth2 and Role-Based Access Control

### CustomOAuth2UserService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/impl/CustomOAuth2UserService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * After Google OAuth2 login:
 * 1. Looks up user by provider + providerId
 * 2. Falls back to email lookup
 * 3. If not found, creates new User with CUSTOMER role
 * 4. Links OAuth2 to existing accounts if not already linked
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");

        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthProviderId(provider, providerId);
        if (existingUser.isEmpty() && email != null) {
            existingUser = userRepository.findByEmailAddress(email);
        }

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthProviderId(providerId);
                userRepository.save(user);
            }
        } else {
            User newUser = User.builder()
                    .emailAddress(email).firstName(firstName).lastName(lastName)
                    .passwordHash(null).isActive(true).role("CUSTOMER")
                    .oauthProvider(provider).oauthProviderId(providerId).build();
            userRepository.save(newUser);
        }

        return oauth2User;
    }
}
```

### OAuth2AuthenticationSuccessHandler.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/handler/OAuth2AuthenticationSuccessHandler.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Issues a JWT after successful OAuth2 login and returns it as JSON.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new RuntimeException("OAuth2 user not found: " + email));

        String token = tokenService.generateToken(user.getId(), user.getRole());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "status", true, "message", "OAuth2 authentication successful",
                "data", Map.of("userId", user.getId().toString(), "role", user.getRole(),
                        "token", token, "email", email)
        ));
    }
}
```

### CustomUserDetailsService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/impl/CustomUserDetailsService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads users from the database for Spring Security AuthenticationManager.
 * Returns UserDetails with ROLE_<role> authorities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + email);
        }

        String role = user.getRole() != null ? user.getRole() : "CUSTOMER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmailAddress())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())))
                .accountExpired(false).accountLocked(false)
                .credentialsExpired(false)
                .disabled(Boolean.FALSE.equals(user.getIsActive()))
                .build();
    }
}
```

### GraphQL Resolvers (@PreAuthorize migration)

All 7 GraphQL resolvers were migrated from `@RequireRoles` (legacy AOP-based, read MDC) to `@PreAuthorize` (Spring Security, reads `SecurityContextHolder`).

**Example — CartResolver.java:**
```java
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
public class CartResolver {
    // ... all methods inherit class-level @PreAuthorize
    
    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")  // Override: only customers can clear cart
    public boolean clearCart(@Argument UUID userId) { ... }
}
```

**Resolvers migrated:**
| Resolver | Class-level | Method overrides |
|----------|-------------|------------------|
| `AddressResolver` | `ADMIN, CUSTOMER` | — |
| `CartResolver` | `ADMIN, CUSTOMER` | `clearCart` → CUSTOMER only |
| `CategoryResolver` | `ADMIN, CUSTOMER` | Mutations → ADMIN only |
| `OrderResolver` | `ADMIN, CUSTOMER` | `orders` → ADMIN; `deleteOrder` → ADMIN; `cancelOrder`/`updatePaymentStatus` → CUSTOMER |
| `ProductResolver` | `ADMIN, CUSTOMER` | All mutations → ADMIN only |
| `ReviewResolver` | `ADMIN, CUSTOMER` | — |
| `UserResolver` | — (method-level) | Queries/mutations split: ADMIN for management, ADMIN+CUSTOMER for own profile |

---

## Epic 5 — DSA and Security Optimization

### TokenBlacklistService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/TokenBlacklistService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DSA: ConcurrentHashMap for O(1) lookup/insert of blacklisted token JTIs.
 * Scheduled purge every 15 minutes removes expired entries.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String jti, Instant expiry) {
        blacklist.put(jti, expiry);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);  // O(1)
    }

    @Scheduled(fixedRate = 900_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    public int size() {
        return blacklist.size();
    }
}
```

### TokenActivityService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/TokenActivityService.java`

```java
/**
 * Tracks token generation, validation, failure, and revocation events in-memory.
 * DSA: ConcurrentHashMap for O(1) token activity updates.
 * Provides TokenUsageStats for admin security report.
 */
@Service
public class TokenActivityService {
    private final ConcurrentMap<String, TokenActivity> activeTokens = new ConcurrentHashMap<>();

    public void logTokenValidation(String token, String userId, String role, String clientIp, String userAgent) { ... }
    public void logTokenGeneration(String token, String userId, String role, String clientIp, String userAgent) { ... }
    public void logTokenValidationFailure(String token, String reason, String clientIp, String userAgent) { ... }
    public void logTokenRevocation(String token, String reason, String clientIp) { ... }
    public TokenUsageStats getTokenUsageStats() { ... }

    public static class TokenActivity { /* tokenId, userId, role, validationCount, timestamps */ }
    public static class TokenUsageStats { /* totalActiveTokens, totalValidations */ }
}
```

### SecurityEventListener.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/config/SecurityEventListener.java`

```java
package com.miracle.smart_ecommerce_security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DSA: AtomicLong for lock-free counters + ConcurrentLinkedDeque capped at 100
 * for O(1) insert/remove bounded event buffer.
 */
@Component
@Slf4j
public class SecurityEventListener {

    private static final int MAX_RECENT_EVENTS = 100;

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong deniedCount = new AtomicLong();
    private final ConcurrentLinkedDeque<SecurityEvent> recentEvents = new ConcurrentLinkedDeque<>();

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        successCount.incrementAndGet();
        addEvent(EventType.AUTH_SUCCESS, event.getAuthentication().getName(),
                event.getAuthentication().getAuthorities().toString());
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        failureCount.incrementAndGet();
        addEvent(EventType.AUTH_FAILURE, event.getAuthentication().getName(), "Bad credentials");
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        deniedCount.incrementAndGet();
        addEvent(EventType.ACCESS_DENIED, event.getAuthentication().get().getName(),
                event.getAuthorizationDecision().toString());
    }

    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }
    public long getDeniedCount()  { return deniedCount.get(); }

    public List<SecurityEvent> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(recentEvents));
    }

    private void addEvent(EventType type, String principal, String detail) {
        recentEvents.addFirst(new SecurityEvent(Instant.now(), type, principal, detail));
        while (recentEvents.size() > MAX_RECENT_EVENTS) recentEvents.removeLast();
    }

    public enum EventType { AUTH_SUCCESS, AUTH_FAILURE, ACCESS_DENIED }
    public record SecurityEvent(Instant timestamp, EventType type, String principal, String detail) {}
}
```

### SecurityReportService.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/service/SecurityReportService.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.miracle.smart_ecommerce_security.config.SecurityEventListener;
import com.miracle.smart_ecommerce_security.config.SecurityEventListener.SecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Aggregates data from SecurityEventListener, TokenBlacklistService, and TokenActivityService.
 * All sources use thread-safe structures (AtomicLong, ConcurrentHashMap, ConcurrentLinkedDeque).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityReportService {

    private final SecurityEventListener securityEventListener;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenActivityService tokenActivityService;

    public SecurityReport generateReport() {
        TokenActivityService.TokenUsageStats tokenStats = tokenActivityService.getTokenUsageStats();
        return new SecurityReport(
                Instant.now(),
                new AuthStats(securityEventListener.getSuccessCount(),
                        securityEventListener.getFailureCount(),
                        securityEventListener.getDeniedCount()),
                new TokenStats(tokenStats.getTotalActiveTokens(),
                        tokenStats.getTotalValidations(),
                        tokenBlacklistService.size()),
                securityEventListener.getRecentEvents()
        );
    }

    public record SecurityReport(Instant generatedAt, AuthStats authenticationStats,
                                  TokenStats tokenStats, List<SecurityEvent> recentSecurityEvents) {}
    public record AuthStats(long totalSuccessfulLogins, long totalFailedLogins, long totalAccessDenials) {}
    public record TokenStats(int activeTokens, long totalValidations, int blacklistedTokens) {}
}
```

### SecurityReportController.java
**Path:** `src/main/java/com/miracle/smart_ecommerce_security/domain/auth/controller/SecurityReportController.java`

```java
package com.miracle.smart_ecommerce_security.domain.auth.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService;
import com.miracle.smart_ecommerce_security.domain.auth.service.SecurityReportService.SecurityReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoint. Protected by URL rule (/api/admin/** → ADMIN) AND @PreAuthorize.
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
    @Operation(summary = "Get security audit report",
            description = "Returns auth stats, token metrics, and last 100 security events.")
    public ResponseEntity<ApiResponse<SecurityReport>> getSecurityReport() {
        SecurityReport report = securityReportService.generateReport();
        return ResponseEntity.ok(ApiResponse.success(report, "Security report generated successfully"));
    }
}
```

---

## Configuration

### application.yaml (security-related)

```yaml
# Spring Security OAuth2 (Google)
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:your-google-client-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:your-google-client-secret}
            scope: openid, profile, email
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://openidconnect.googleapis.com/v1/userinfo
            user-name-attribute: sub

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:c2VjdXJlLWVjb21tZXJjZS1qd3Qtc2VjcmV0LWtleS0yMDI2LXNtYXJ0LWVjb21tZXJjZS1zZWN1cml0eQ==}
  expiration-ms: ${JWT_EXPIRATION:86400000}  # 24 hours
```

### V2__Add_oauth_provider_columns.sql
**Path:** `src/main/resources/db/migration/V2__Add_oauth_provider_columns.sql`

```sql
ALTER TABLE app_user ADD COLUMN oauth_provider VARCHAR(50);
ALTER TABLE app_user ADD COLUMN oauth_provider_id VARCHAR(255);
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;
CREATE INDEX idx_user_oauth ON app_user(oauth_provider, oauth_provider_id);
```

---

## Unit Tests (25 passing)

### JwtTokenServiceTest.java

| Test | What it verifies |
|------|-----------------|
| `generateToken_returnsValidJwt` | Token is non-null, 3-part JWT string |
| `generateToken_containsCorrectClaims` | sub=userId, role=ADMIN round-trips |
| `validateToken_success` | Fresh token → AuthPrincipal with correct values |
| `validateToken_rejectsNull` | null → empty Optional |
| `validateToken_rejectsBlank` | blank → empty Optional |
| `validateToken_rejectsTamperedToken` | Flipped signature chars → rejected |
| `validateToken_rejectsExpiredToken` | 0ms expiry → rejected immediately |
| `validateToken_rejectsBlacklistedToken` | Mock blacklist returns true → rejected |
| `validateToken_rejectsDifferentSigningKey` | Token from other key → rejected |
| `extractJti_returnsJti` | Valid token → non-blank JTI |
| `extractJti_emptyForInvalidToken` | Invalid string → empty Optional |

### TokenBlacklistServiceTest.java

| Test | What it verifies |
|------|-----------------|
| `blacklist_marksTokenAsBlacklisted` | Put + check → true, size=1 |
| `isBlacklisted_returnsFalseForUnknown` | Unknown JTI → false |
| `blacklist_multipleTokens` | 3 tokens independently tracked |
| `purgeExpired_removesExpiredTokens` | Past-expiry removed, future-expiry kept |
| `purgeExpired_noOpWhenNoneExpired` | No change when all valid |
| `size_zeroWhenEmpty` | Initial size = 0 |

### SecurityEventListenerTest.java

| Test | What it verifies |
|------|-----------------|
| `onSuccess_incrementsCounter` | Success count=1, event recorded |
| `onFailure_incrementsCounter` | Failure count=1, event recorded |
| `events_newestFirst` | 5 events → newest at index 0 |
| `events_boundedBuffer` | 120 events → buffer capped at 100 |
| `events_unmodifiable` | Returned list throws on modification |
| `initialState` | All counters=0, events empty |

### SecurityReportServiceTest.java

| Test | What it verifies |
|------|-----------------|
| `generateReport_aggregatesAllSources` | All 3 data sources combined correctly |
| `generateReport_includesRecentEvents` | Events from listener included in report |

---

## Deleted Legacy Files

| File | Reason |
|------|--------|
| `ConsolidatedAuthFilter.java` | Deprecated. Used X-Auth-Token/X-User-Id headers. Replaced by `JwtAuthenticationFilter` + Spring Security `SecurityFilterChain`. |
| `SimpleTokenService.java` | Deprecated. Used UUID-based tokens. Replaced by `JwtTokenService` (HMAC-SHA256). |
| `AuthorizationAspect.java` | Deprecated. MDC-based role checking via AOP. Replaced by `@PreAuthorize` annotations. |
| `AuthService.java` | Interface superseded by `AuthenticationManager` in `AuthController`. |
| `AuthServiceImpl.java` | Implementation that bypassed Spring Security. Replaced by `AuthenticationManager.authenticate()`. |
| `ApiCorsConfig.java` | Duplicate CORS configuration. Consolidated into `SecurityConfig.corsConfigurationSource()`. |

---

## Change Summary

| Category | Count | Details |
|----------|-------|---------|
| **Files deleted** | 6 | Legacy auth filter, simple token service, authorization aspect, auth service interface+impl, duplicate CORS config |
| **Files created** | 8 | CsrfDemoController, SecurityReportController, SecurityReportService, csrf-demo.html, 4 test classes |
| **Files updated** | 12 | SecurityConfig, SecurityEventListener, JwtTokenService, RequireRoles (deprecated), 7 GraphQL resolvers, README.md, AUTH_ARCHITECTURE.md |
| **Tests** | 25 | All pass ✅ — JwtTokenService (11), TokenBlacklist (6), SecurityEventListener (6), SecurityReport (2) |
| **Build** | ✅ | `mvn compile` + `mvn test` — BUILD SUCCESS |

