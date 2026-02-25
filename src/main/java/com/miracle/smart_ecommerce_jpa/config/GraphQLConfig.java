package com.miracle.smart_ecommerce_jpa.config;

import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.service.UserService;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;


import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import org.slf4j.MDC;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;
import java.util.Optional;
import java.util.UUID;


/**
 * GraphQL Configuration
 * Registers custom scalar types for GraphQL schema
 */
@Configuration
@RequiredArgsConstructor
public class GraphQLConfig {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        // Create BigDecimal scalar with the exact name used in schema
        GraphQLScalarType bigDecimalScalar = GraphQLScalarType.newScalar()
                .name("BigDecimal")
                .coercing(ExtendedScalars.GraphQLBigDecimal.getCoercing())
                .build();

        // Create OffsetDateTime scalar with the exact name used in schema
        GraphQLScalarType offsetDateTimeScalar = GraphQLScalarType.newScalar()
                .name("OffsetDateTime")
                .coercing(ExtendedScalars.DateTime.getCoercing())
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(offsetDateTimeScalar)
                .scalar(bigDecimalScalar)
                .build();
    }



    @Bean
    public WebGraphQlInterceptor authInterceptor() {
        return (request, chain) -> {
            String token = request.getHeaders().getFirst("X-Auth-Token");
            String userIdHeader = request.getHeaders().getFirst("X-User-Id");

            if (token != null && !token.isBlank()) {
                Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(token.trim());
                if (principal.isPresent()) {
                    MDC.put("userId", principal.get().userId.toString());
                    MDC.put("userRole", principal.get().role);
                }
            } else if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    UUID id = UUID.fromString(userIdHeader);
                    Optional<User> maybe = userRepository.findById(id);
                    if (maybe.isPresent()) {
                        User u = maybe.get();
                        MDC.put("userId", u.getId().toString());
                        MDC.put("userRole", u.getRole() != null ? u.getRole() : "CUSTOMER");
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            return chain.next(request).doFinally(signal -> {
                MDC.remove("userId");
                MDC.remove("userRole");
            });
        };
    }
}

