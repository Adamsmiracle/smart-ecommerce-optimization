package com.miracle.smart_ecommerce_security.config;

import com.miracle.smart_ecommerce_security.domain.auth.filter.JwtAuthenticationFilter;
import com.miracle.smart_ecommerce_security.domain.auth.handler.AuthenticationFailureHandler;
import com.miracle.smart_ecommerce_security.domain.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.CustomOAuth2UserService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.CustomUserDetailsService;
import com.miracle.smart_ecommerce_security.domain.auth.exception.JwtAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

/**
 * Central Spring Security configuration for the Smart E-Commerce API.
 *
 * <h3>Architecture decisions</h3>
 * <ul>
 *   <li><b>CSRF</b> — Disabled for stateless JWT API paths ({@code /api/**}, {@code /graphql}).
 *       Enabled with {@link CookieCsrfTokenRepository} for the {@code /csrf-demo} Thymeleaf
 *       form endpoint to demonstrate CSRF protection for stateful browser interactions.</li>
 *   <li><b>Sessions</b> — {@code STATELESS}; no server-side session. Every request carries a JWT.</li>
 *   <li><b>CORS</b> — Configured here so Spring Security processes preflight (OPTIONS) requests
 *       correctly. Allows specific origins for frontend apps and Postman.</li>
 *   <li><b>Method security</b> — {@code @EnableMethodSecurity} enables {@code @PreAuthorize} /
 *       {@code @Secured} on controller and GraphQL resolver methods for role-based access control.</li>
 *   <li><b>JWT filter</b> — Registered <em>before</em> {@code UsernamePasswordAuthenticationFilter}
 *       to populate {@code SecurityContextHolder} from the Bearer token.</li>
 *   <li><b>OAuth2</b> — Google login configured with a custom {@code OAuth2UserService} and
 *       success handler that issues a JWT after successful authentication.</li>
 *   <li><b>Password hashing</b> — BCrypt via {@link PasswordConfig}.</li>
 * </ul>
 *
 * <h3>Roles</h3>
 * Two roles are defined: {@code ADMIN}, {@code CUSTOMER}.
 * <ul>
 *   <li>{@code ADMIN} — full access to everything including security reports and destructive deletes</li>
 *   <li>{@code CUSTOMER} — end-user; own profile, cart, orders, reviews</li>
 * </ul>
 * Endpoints are annotated with {@code @PreAuthorize} to enforce role-based access.
 *
 * @see JwtAuthenticationFilter
 * @see com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService
 * @see SecurityEventListener
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;

    // Inject the Entry Point here
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/graphql", "/login/oauth2/code/*")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Changed to STATELESS for optimized JWT handling
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/auth/token/inspect").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/graphiql/**", "/graphiql", "/graphql").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/", "/error", "/login/**", "/oauth2/**", "/csrf-demo/**").permitAll()
                        .requestMatchers("/favicon.ico", "/favicon.png", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                )

                .exceptionHandling(ex -> ex
                        // Use the injected bean here
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    // Extracted AccessDeniedHandler for cleaner code
    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            String body = String.format(
                    "{\"status\":false,\"message\":\"Access denied. Insufficient privileges.\",\"statusCode\":403,\"timestamp\":\"%s\"}",
                    java.time.Instant.now()
            );
            response.getWriter().write(body);
        };
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
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}